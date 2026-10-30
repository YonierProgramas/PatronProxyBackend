package edu.monitor;

import static org.junit.jupiter.api.Assertions.*;

import edu.monitor.business.*;
import edu.monitor.contract.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
  @Test
  void deterministicSuccessAndRetrieve() {
    var service = new PaymentService(() -> 0.10);
    var payment =
        (Map<?, ?>) service.execute("processPayment", Map.of("orderId", "DEMO", "amount", "12.50"));
    assertEquals(new BigDecimal("12.50"), payment.get("amount"));
    assertEquals(
        payment, service.execute("getPayment", Map.of("paymentId", payment.get("paymentId"))));
  }

  @Test
  void deterministicFailureBeforePersisting() {
    var random = new AtomicInteger();
    var service = new PaymentService(() -> random.getAndIncrement() == 0 ? 0.099 : 0.9);
    assertEquals(
        "SIMULATED_FAILURE",
        assertThrows(
                ServiceException.class,
                () -> service.execute("processPayment", Map.of("orderId", "DEMO", "amount", 12)))
            .code());
    var payment =
        (Map<?, ?>) service.execute("processPayment", Map.of("orderId", "DEMO", "amount", 12));
    assertEquals("SUCCESS", payment.get("status"));
  }

  @Test
  void validRetrievalCanFailAndInvalidCallsDoNotSample() {
    var count = new AtomicInteger();
    var service =
        new PaymentService(
            () -> {
              count.incrementAndGet();
              return 0;
            });
    assertThrows(
        ServiceException.class,
        () -> service.execute("processPayment", Map.of("orderId", "D", "amount", 0)));
    assertThrows(
        ServiceException.class,
        () -> service.execute("getPayment", Map.of("paymentId", "missing")));
    assertThrows(ServiceException.class, () -> service.execute("unknown", Map.of()));
    assertEquals(0, count.get());
    var calls = new AtomicInteger();
    var another = new PaymentService(() -> calls.getAndIncrement() == 0 ? 0.9 : 0);
    var p = (Map<?, ?>) another.execute("processPayment", Map.of("orderId", "D", "amount", 5));
    assertEquals(
        "SIMULATED_FAILURE",
        assertThrows(
                ServiceException.class,
                () -> another.execute("getPayment", Map.of("paymentId", p.get("paymentId"))))
            .code());
  }

  @Test
  void moneyValidation() {
    for (Object amount : List.of(-1, 0, "NaN", "1.001", "100000001"))
      assertThrows(ServiceException.class, () -> Inputs.amount(Map.of("amount", amount)));
  }
}
