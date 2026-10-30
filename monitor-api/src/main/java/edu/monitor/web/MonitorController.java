package edu.monitor.web;

import edu.monitor.audit.*;
import edu.monitor.contract.*;
import edu.monitor.metrics.*;
import edu.monitor.proxy.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MonitorController {
  private final ServiceGateway gateway;
  private final AuditStore store;
  private final SimulationService simulation;

  public MonitorController(ServiceGateway gateway, AuditStore store, SimulationService simulation) {
    this.gateway = gateway;
    this.store = store;
    this.simulation = simulation;
  }

  @PostMapping("/services/{service}/{operation}")
  public OperationResponse execute(
      @PathVariable String service,
      @PathVariable String operation,
      @Valid @RequestBody OperationRequest request) {
    return gateway.execute(service, operation, request);
  }

  @GetMapping("/metrics/summary")
  public List<Map<String, Object>> summary() {
    return store.summary();
  }

  @GetMapping("/metrics/logs")
  public Map<String, Object> logs(
      @RequestParam(required = false) String service,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return store.logs(service, status, from, to, page, size);
  }

  @PostMapping("/metrics/simulate-load")
  public Map<String, Object> simulate() {
    return simulation.simulate();
  }
}
