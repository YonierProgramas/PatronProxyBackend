package edu.monitor.business;

import edu.monitor.contract.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
  private final Map<String, Map<String, Object>> payments = new ConcurrentHashMap<>();
  private final FailureRandom random;

  public PaymentService(FailureRandom random) {
    this.random = random;
  }

  public Object execute(String operation, Map<String, Object> p) {
    // Validation precedes sampling, including existence checks for getPayment.
    if (operation.equals("processPayment")) {
      String orderId = Inputs.text(p, "orderId");
      var amount = Inputs.amount(p);
      maybeFail();
      String id = UUID.randomUUID().toString();
      Map<String, Object> payment =
          Map.of(
              "paymentId",
              id,
              "orderId",
              orderId,
              "amount",
              amount,
              "status",
              "SUCCESS",
              "createdAt",
              Instant.now().toString());
      payments.put(id, payment);
      return payment;
    }
    if (operation.equals("getPayment")) {
      var payment = payments.get(Inputs.text(p, "paymentId"));
      if (payment == null) throw Inputs.missing();
      maybeFail();
      return payment;
    }
    throw Inputs.unknown();
  }

  private void maybeFail() {
    if (random.nextDouble() < 0.10)
      throw new ServiceException(503, "SIMULATED_FAILURE", "Fallo académico simulado en pagos.");
  }
}
