package edu.monitor.business;

import edu.monitor.contract.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
  private final Map<String, Map<String, Object>> orders = new ConcurrentHashMap<>();

  public Object execute(String operation, Map<String, Object> p) {
    return switch (operation) {
      case "createOrder" -> {
        String productId = Inputs.text(p, "productId");
        int quantity = Inputs.quantity(p);
        String id = UUID.randomUUID().toString();
        Map<String, Object> order =
            Map.of(
                "orderId",
                id,
                "productId",
                productId,
                "quantity",
                quantity,
                "createdAt",
                Instant.now().toString());
        orders.put(id, order);
        yield order;
      }
      case "getOrder" -> {
        var order = orders.get(Inputs.text(p, "orderId"));
        if (order == null) throw Inputs.missing();
        yield order;
      }
      case "listOrders" ->
          orders.values().stream()
              .sorted(Comparator.comparing(o -> o.get("orderId").toString()))
              .toList();
      default -> throw Inputs.unknown();
    };
  }
}
