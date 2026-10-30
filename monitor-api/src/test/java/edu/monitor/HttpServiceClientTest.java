package edu.monitor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import edu.monitor.audit.*;
import edu.monitor.contract.*;
import edu.monitor.proxy.*;
import java.net.*;
import java.util.*;
import org.junit.jupiter.api.*;

class HttpServiceClientTest {
  @Test
  void realHttpSuccessAndInternalRequestId() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/internal/getStock",
        exchange -> {
          assertNotNull(UUID.fromString(exchange.getRequestHeaders().getFirst("X-Request-Id")));
          var body = "{\"requestId\":\"test\",\"response\":{\"stock\":30}}".getBytes();
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      var client =
          new HttpServiceClient(
              "http://127.0.0.1:" + server.getAddress().getPort(),
              500,
              new ObjectMapper(),
              Set.of("getStock"));
      assertEquals(Map.of("stock", 30), client.execute("getStock", Map.of("productId", "SKU-001")));
      assertThrows(ServiceException.class, () -> client.execute("unknown"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void timeoutAndUnavailable() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/internal/getStock",
        exchange -> {
          try {
            Thread.sleep(300);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          exchange.close();
        });
    server.start();
    int port = server.getAddress().getPort();
    try {
      var client =
          new HttpServiceClient(
              "http://127.0.0.1:" + port, 50, new ObjectMapper(), Set.of("getStock"));
      assertEquals(
          504, assertThrows(ServiceException.class, () -> client.execute("getStock")).status());
    } finally {
      server.stop(0);
    }
    var client =
        new HttpServiceClient(
            "http://127.0.0.1:" + port, 100, new ObjectMapper(), Set.of("getStock"));
    assertEquals(
        503, assertThrows(ServiceException.class, () -> client.execute("getStock")).status());
  }

  @Test
  void allThreeGatewayServicesUseProxy() {
    var store = mock(AuditStore.class);
    var gateway =
        new ServiceGateway(
            store,
            new ObjectMapper(),
            "http://127.0.0.1:1",
            "http://127.0.0.1:1",
            "http://127.0.0.1:1",
            50);
    for (String id : List.of("inventory", "orders", "payments"))
      assertThrows(
          ServiceException.class,
          () -> gateway.execute(id, "unknown", new OperationRequest(List.of())));
    var logs = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
    verify(store, times(3)).save(logs.capture());
    assertEquals(
        Set.of("inventory", "orders", "payments"),
        new HashSet<>(logs.getAllValues().stream().map(AuditLog::serviceId).toList()));
  }
}
