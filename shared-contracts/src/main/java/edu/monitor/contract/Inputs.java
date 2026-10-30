package edu.monitor.contract;

import java.math.BigDecimal;
import java.util.Map;

public final class Inputs {
  private Inputs() {}

  public static String text(Map<String, Object> input, String key) {
    Object value = input.get(key);
    if (!(value instanceof String s) || s.isBlank() || s.length() > 100)
      throw invalid("Identificador o texto inválido: " + key);
    return s.trim();
  }

  public static int quantity(Map<String, Object> input) {
    try {
      int n = new BigDecimal(String.valueOf(input.get("quantity"))).intValueExact();
      if (n > 0 && n <= 100000) return n;
    } catch (ArithmeticException | NumberFormatException ignored) {
    }
    throw invalid("La cantidad debe ser un entero entre 1 y 100000.");
  }

  public static BigDecimal amount(Map<String, Object> input) {
    try {
      BigDecimal n = new BigDecimal(String.valueOf(input.get("amount")));
      if (n.signum() > 0 && n.scale() <= 2 && n.compareTo(new BigDecimal("100000000")) <= 0)
        return n;
    } catch (NumberFormatException ignored) {
    }
    throw invalid("El importe debe ser positivo, hasta 100000000 y con máximo dos decimales.");
  }

  public static Map<String, Object> params(OperationRequest request, boolean required) {
    if (request == null || request.params() == null || request.params().size() > 1)
      throw invalid("Utiliza params con cero o un objeto.");
    if (required && request.params().isEmpty()) throw invalid("Faltan los parámetros.");
    if (!request.params().isEmpty() && request.params().getFirst() == null)
      throw invalid("Parámetros inválidos.");
    return request.params().isEmpty() ? Map.of() : request.params().getFirst();
  }

  public static ServiceException invalid(String message) {
    return new ServiceException(400, "INVALID_INPUT", message);
  }

  public static ServiceException missing() {
    return new ServiceException(404, "NOT_FOUND", "Recurso inexistente.");
  }

  public static ServiceException unknown() {
    return new ServiceException(400, "UNKNOWN_OPERATION", "Operación desconocida.");
  }
}
