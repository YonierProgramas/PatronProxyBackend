package edu.monitor.metrics;

import edu.monitor.contract.*;
import edu.monitor.proxy.ServiceGateway;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SimulationService {
  private final ServiceGateway gateway;
  private final AtomicBoolean running = new AtomicBoolean();
  private final long cooldownNanos;
  private volatile long lastStart;
  private volatile boolean hasStarted;

  public SimulationService(
      ServiceGateway gateway, @Value("${simulation.cooldown.ms}") long cooldown) {
    this.gateway = gateway;
    this.cooldownNanos = cooldown * 1_000_000;
  }

  public Map<String, Object> simulate() {
    if (!running.compareAndSet(false, true))
      throw new ServiceException(409, "SIMULATION_RUNNING", "Ya hay una simulación en ejecución.");
    String previous = MDC.get("requestId");
    try {
      long now = System.nanoTime();
      if (hasStarted && now - lastStart < cooldownNanos)
        throw new ServiceException(
            429, "SIMULATION_RATE_LIMIT", "Espera antes de repetir la simulación.");
      lastStart = now;
      hasStarted = true;
      String batchId = UUID.randomUUID().toString();
      Instant started = Instant.now();
      int success = 0, error = 0;
      // Sequential concurrency is deliberately bounded to one: 50 real calls on small machines.
      for (int i = 0; i < 50; i++) {
        MDC.put("requestId", UUID.randomUUID().toString());
        String service =
            switch (i % 3) {
              case 0 -> "inventory";
              case 1 -> "orders";
              default -> "payments";
            };
        String operation =
            switch (service) {
              case "inventory" -> "getStock";
              case "orders" -> "createOrder";
              default -> "processPayment";
            };
        Map<String, Object> p =
            switch (service) {
              case "inventory" -> Map.of("productId", "SKU-001");
              case "orders" -> Map.of("productId", "SKU-002", "quantity", 1);
              default -> Map.of("orderId", "DEMO-ORDER", "amount", 12.50);
            };
        try {
          gateway.execute(service, operation, new OperationRequest(List.of(p)));
          success++;
        } catch (RuntimeException e) {
          error++;
        }
      }
      return Map.of(
          "batchId",
          batchId,
          "requestedCalls",
          50,
          "completedCalls",
          success + error,
          "successCount",
          success,
          "errorCount",
          error,
          "startedAt",
          started,
          "finishedAt",
          Instant.now());
    } finally {
      if (previous == null) MDC.remove("requestId");
      else MDC.put("requestId", previous);
      running.set(false);
    }
  }
}
