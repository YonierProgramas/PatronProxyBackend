package edu.monitor.business;

import edu.monitor.contract.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
  private final Map<String, Integer> stock =
      new TreeMap<>(Map.of("SKU-001", 30, "SKU-002", 20, "SKU-003", 40));

  // A single lock makes stock validation and mutation atomic.
  public synchronized Object execute(String operation, Map<String, Object> p) {
    return switch (operation) {
      case "listProducts" ->
          stock.entrySet().stream()
              .map(e -> Map.of("productId", e.getKey(), "stock", e.getValue()))
              .toList();
      case "getStock" -> {
        String id = Inputs.text(p, "productId");
        yield product(id);
      }
      case "addStock", "removeStock" -> {
        String id = Inputs.text(p, "productId");
        int quantity = Inputs.quantity(p);
        product(id);
        int next = stock.get(id) + (operation.equals("addStock") ? quantity : -quantity);
        if (next < 0)
          throw new ServiceException(409, "INSUFFICIENT_STOCK", "Existencias insuficientes.");
        if (next > 1000000)
          throw new ServiceException(409, "STOCK_LIMIT", "Límite de existencias alcanzado.");
        stock.put(id, next);
        yield product(id);
      }
      default -> throw Inputs.unknown();
    };
  }

  private Map<String, Object> product(String id) {
    if (!stock.containsKey(id)) throw Inputs.missing();
    return Map.of("productId", id, "stock", stock.get(id));
  }
}
