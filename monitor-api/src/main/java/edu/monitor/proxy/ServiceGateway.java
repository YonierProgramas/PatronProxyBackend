package edu.monitor.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.monitor.audit.*;
import edu.monitor.contract.*;
import edu.monitor.web.RequestContext;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceGateway {
  private final Map<String, MicroserviceProxy<Object>> proxies;

  public ServiceGateway(
      AuditStore store,
      ObjectMapper mapper,
      @Value("${inventory.service.url}") String inventory,
      @Value("${order.service.url}") String orders,
      @Value("${payment.service.url}") String payments,
      @Value("${internal.timeout.ms}") int timeout,
      @Value("${internal.service.token:}") String internalToken) {
    proxies =
        Map.of(
            "inventory",
            new LoggingProxy<>(
                "inventory",
                new HttpServiceClient(
                    inventory,
                    timeout,
                    mapper,
                    Set.of("listProducts", "getStock", "addStock", "removeStock"),
                    internalToken),
                store),
            "orders",
            new LoggingProxy<>(
                "orders",
                new HttpServiceClient(
                    orders,
                    timeout,
                    mapper,
                    Set.of("createOrder", "getOrder", "listOrders"),
                    internalToken),
                store),
            "payments",
            new LoggingProxy<>(
                "payments",
                new HttpServiceClient(
                    payments,
                    timeout,
                    mapper,
                    Set.of("processPayment", "getPayment"),
                    internalToken),
                store));
  }

  public OperationResponse execute(String service, String operation, OperationRequest request) {
    var proxy = proxies.get(service);
    if (proxy == null) throw Inputs.missing();
    if (operation.length() > 100) throw Inputs.invalid("Operación demasiado larga.");
    Inputs.params(request, false);
    Object result = proxy.execute(operation, request.params().toArray());
    return new OperationResponse(RequestContext.id(), result);
  }
}
