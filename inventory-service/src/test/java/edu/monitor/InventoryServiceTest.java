package edu.monitor;

import static org.junit.jupiter.api.Assertions.*;

import edu.monitor.business.InventoryService;
import edu.monitor.contract.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {
  @Test
  void operationsAndValidation() {
    var service = new InventoryService();
    assertEquals(3, ((List<?>) service.execute("listProducts", Map.of())).size());
    assertEquals(
        33,
        ((Map<?, ?>) service.execute("addStock", Map.of("productId", "SKU-001", "quantity", 3)))
            .get("stock"));
    assertEquals(
        31,
        ((Map<?, ?>) service.execute("removeStock", Map.of("productId", "SKU-001", "quantity", 2)))
            .get("stock"));
    assertEquals(
        409,
        assertThrows(
                ServiceException.class,
                () ->
                    service.execute("removeStock", Map.of("productId", "SKU-001", "quantity", 100)))
            .status());
    for (Object q : List.of(0, -1, 1.2, "x", 100001))
      assertThrows(
          ServiceException.class,
          () -> service.execute("addStock", Map.of("productId", "SKU-001", "quantity", q)));
    assertThrows(
        ServiceException.class, () -> service.execute("getStock", Map.of("productId", " ")));
    assertEquals(
        404,
        assertThrows(
                ServiceException.class,
                () -> service.execute("getStock", Map.of("productId", "missing")))
            .status());
    assertThrows(ServiceException.class, () -> service.execute("unknown", Map.of()));
  }

  @Test
  void concurrentMutationIsAtomic() throws Exception {
    var service = new InventoryService();
    try (var pool = Executors.newFixedThreadPool(2)) {
      var calls = new ArrayList<Callable<Object>>();
      for (int i = 0; i < 100; i++)
        calls.add(() -> service.execute("addStock", Map.of("productId", "SKU-001", "quantity", 1)));
      for (var future : pool.invokeAll(calls)) future.get();
    }
    assertEquals(
        130,
        ((Map<?, ?>) service.execute("getStock", Map.of("productId", "SKU-001"))).get("stock"));
  }
}
