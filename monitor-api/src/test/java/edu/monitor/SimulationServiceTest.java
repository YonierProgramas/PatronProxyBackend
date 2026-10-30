package edu.monitor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.monitor.contract.*;
import edu.monitor.metrics.*;
import edu.monitor.proxy.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class SimulationServiceTest {
  @Test
  void exactlyFiftyCallsContinueOnFailureAndUniqueIds() {
    var gateway = mock(ServiceGateway.class);
    Set<String> ids = new HashSet<>();
    when(gateway.execute(anyString(), anyString(), any()))
        .thenAnswer(
            call -> {
              ids.add(edu.monitor.web.RequestContext.id());
              if (call.getArgument(0).equals("payments"))
                throw new ServiceException(503, "SIMULATED_FAILURE", "failure");
              return new OperationResponse(edu.monitor.web.RequestContext.id(), Map.of());
            });
    var simulation = new SimulationService(gateway, 10000);
    var result = simulation.simulate();
    assertEquals(50, result.get("completedCalls"));
    assertEquals(16, result.get("errorCount"));
    assertEquals(34, result.get("successCount"));
    assertEquals(50, ids.size());
    verify(gateway, times(50)).execute(anyString(), anyString(), any());
    assertEquals(429, assertThrows(ServiceException.class, simulation::simulate).status());
  }

  @Test
  void overlappingSimulationRejected() throws Exception {
    var gateway = mock(ServiceGateway.class);
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    when(gateway.execute(anyString(), anyString(), any()))
        .thenAnswer(
            call -> {
              entered.countDown();
              release.await(5, TimeUnit.SECONDS);
              return new OperationResponse("x", null);
            });
    var simulation = new SimulationService(gateway, 0);
    try (var pool = Executors.newSingleThreadExecutor()) {
      var future = pool.submit(simulation::simulate);
      assertTrue(entered.await(5, TimeUnit.SECONDS));
      try {
        assertEquals(409, assertThrows(ServiceException.class, simulation::simulate).status());
      } finally {
        release.countDown();
      }
      future.get(10, TimeUnit.SECONDS);
    }
  }
}
