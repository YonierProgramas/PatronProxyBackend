package edu.monitor.web;

import edu.monitor.business.InventoryService;
import edu.monitor.contract.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class BusinessController {
  private final InventoryService service;

  public BusinessController(InventoryService service) {
    this.service = service;
  }

  @PostMapping("/internal/{operation}")
  public OperationResponse execute(
      @PathVariable String operation, @Valid @RequestBody OperationRequest request) {
    return new OperationResponse(
        RequestContext.id(),
        service.execute(operation, Inputs.params(request, !operation.startsWith("list"))));
  }
}
