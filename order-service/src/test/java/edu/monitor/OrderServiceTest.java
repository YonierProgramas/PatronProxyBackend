package edu.monitor;

import static org.junit.jupiter.api.Assertions.*;

import edu.monitor.business.OrderService;
import edu.monitor.contract.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OrderServiceTest {
  @Test
  void createRetrieveListAndValidate() {
    var service = new OrderService();
    var order =
        (Map<?, ?>) service.execute("createOrder", Map.of("productId", "SKU-001", "quantity", 2));
    assertNotNull(UUID.fromString(order.get("orderId").toString()));
    assertEquals(order, service.execute("getOrder", Map.of("orderId", order.get("orderId"))));
    assertEquals(1, ((List<?>) service.execute("listOrders", Map.of())).size());
    assertThrows(
        ServiceException.class,
        () -> service.execute("createOrder", Map.of("productId", "x", "quantity", 0)));
    assertThrows(
        ServiceException.class, () -> service.execute("getOrder", Map.of("orderId", "unknown")));
    assertThrows(ServiceException.class, () -> service.execute("x", Map.of()));
  }
}
