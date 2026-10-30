package edu.monitor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.monitor.audit.*;
import edu.monitor.contract.*;
import edu.monitor.proxy.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

class LoggingProxyTest {
  @Test
  void successDelegatesExactlyOnceAndRecordsOneFinalEntry() {
    AuditStore store = mock(AuditStore.class);
    MicroserviceProxy<Object> delegate = mock(MicroserviceProxy.class);
    Object result = new Object();
    when(delegate.execute("read", "x")).thenReturn(result);
    var proxy = new LoggingProxy<>("inventory", delegate, store);
    assertSame(result, proxy.execute("read", "x"));
    verify(delegate, times(1)).execute("read", "x");
    var captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(store, times(1)).save(captor.capture());
    var log = captor.getValue();
    assertEquals("SUCCESS", log.status());
    assertTrue(log.durationMs() >= 0);
    assertNotNull(UUID.fromString(log.requestId()));
    assertEquals(List.of("x"), Arrays.asList((Object[]) log.inputParams()));
  }

  @Test
  void errorIsRecordedAndPropagated() {
    AuditStore store = mock(AuditStore.class);
    MicroserviceProxy<Object> delegate = mock(MicroserviceProxy.class);
    var failure = new ServiceException(409, "CONFLICT", "Conflicto");
    when(delegate.execute("fail")).thenThrow(failure);
    var proxy = new LoggingProxy<>("payments", delegate, store);
    assertSame(failure, assertThrows(ServiceException.class, () -> proxy.execute("fail")));
    var captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(store).save(captor.capture());
    assertEquals("ERROR", captor.getValue().status());
    assertTrue(captor.getValue().stackTraceSummary().contains("CONFLICT"));
    verify(delegate, times(1)).execute("fail");
  }

  @Test
  void idsAreUnique() {
    AuditStore store = mock(AuditStore.class);
    var proxy = new LoggingProxy<>("orders", (op, p) -> null, store);
    proxy.execute("a");
    proxy.execute("b");
    var captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(store, times(2)).save(captor.capture());
    assertNotEquals(
        captor.getAllValues().get(0).requestId(), captor.getAllValues().get(1).requestId());
  }

  @Test
  void auditFailureDoesNotRetryBusiness() {
    AuditStore store = mock(AuditStore.class);
    MicroserviceProxy<Object> delegate = mock(MicroserviceProxy.class);
    doThrow(new ServiceException(500, "AUDIT_PERSISTENCE_FAILED", "audit failed"))
        .when(store)
        .save(any());
    assertThrows(
        ServiceException.class,
        () -> new LoggingProxy<>("orders", delegate, store).execute("create"));
    verify(delegate, times(1)).execute("create");
  }
}
