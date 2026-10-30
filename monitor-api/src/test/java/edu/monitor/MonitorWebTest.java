package edu.monitor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.monitor.audit.*;
import edu.monitor.contract.*;
import edu.monitor.metrics.*;
import edu.monitor.proxy.*;
import edu.monitor.web.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MonitorController.class)
@Import({RequestFilter.class, ErrorHandler.class, CorsConfiguration.class})
class MonitorWebTest {
  @Autowired MockMvc mvc;
  @MockitoBean ServiceGateway gateway;
  @MockitoBean AuditStore store;
  @MockitoBean SimulationService simulation;

  @Test
  void createsPublicUuidAndIgnoresUntrustedHeader() throws Exception {
    when(gateway.execute(anyString(), anyString(), any()))
        .thenAnswer(call -> new OperationResponse(RequestContext.id(), Map.of("stock", 30)));
    mvc.perform(
            post("/api/services/inventory/getStock")
                .header("X-Request-Id", "user-controlled")
                .contentType("application/json")
                .content("{\"params\":[{\"productId\":\"SKU-001\"}]}"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.requestId").value(org.hamcrest.Matchers.matchesPattern("[0-9a-f-]{36}")))
        .andExpect(jsonPath("$.response.stock").value(30));
  }

  @Test
  void errorsHaveUuidAndTypedStatus() throws Exception {
    when(gateway.execute(anyString(), anyString(), any()))
        .thenThrow(new ServiceException(409, "INSUFFICIENT_STOCK", "Existencias insuficientes."));
    mvc.perform(
            post("/api/services/inventory/removeStock")
                .contentType("application/json")
                .content("{\"params\":[]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.requestId").isNotEmpty())
        .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
  }

  @Test
  void malformedAndOversizedBodyRejectedBeforeBusiness() throws Exception {
    mvc.perform(
            post("/api/services/orders/createOrder")
                .contentType("application/json")
                .content("{\"params\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.requestId").isNotEmpty());
    mvc.perform(
            post("/api/services/orders/createOrder")
                .contentType("application/json")
                .content("x".repeat(32769)))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.errorCode").value("BODY_TOO_LARGE"));
    verifyNoInteractions(gateway);
  }

  @Test
  void explicitCorsAndNoMetricsBusinessCalls() throws Exception {
    when(store.summary()).thenReturn(List.of());
    mvc.perform(get("/api/metrics/summary").header("Origin", "http://localhost:5173"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    mvc.perform(
            options("/api/metrics/summary")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(gateway);
  }

  @Test
  void invalidIsoDateIsBadRequest() throws Exception {
    mvc.perform(get("/api/metrics/logs").param("from", "invalid"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(store);
  }
}
