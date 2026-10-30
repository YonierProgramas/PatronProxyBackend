package edu.monitor;

import static org.junit.jupiter.api.Assertions.*;

import edu.monitor.audit.*;
import edu.monitor.contract.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuditStoreTest {
  @Autowired AuditStore store;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clear() {
    jdbc.update("DELETE FROM audit_log");
  }

  void save(String service, String status, int index) {
    store.save(
        new AuditLog(
            service,
            "getStock",
            index,
            status,
            UUID.randomUUID().toString(),
            Instant.parse("2026-01-01T00:00:00Z").plusSeconds(index),
            List.of(Map.of("productId", "SKU-001")),
            Map.of("stock", 30),
            status.equals("ERROR") ? "TEST_ERROR" : null,
            null,
            null));
  }

  @Test
  void emptySummary() {
    var summary = store.summary();
    assertEquals(3, summary.size());
    for (var s : summary) {
      assertEquals(0L, s.get("totalCalls"));
      assertNull(s.get("successRate"));
      assertNull(s.get("errorRate"));
      assertNull(s.get("averageDurationMs"));
    }
  }

  @Test
  void persistedLogsFiltersPaginationAndInclusiveDates() {
    for (int i = 0; i < 6; i++) save("inventory", i % 2 == 0 ? "SUCCESS" : "ERROR", i);
    save("orders", "ERROR", 3);
    var result =
        store.logs(
            "inventory",
            "ERROR",
            Instant.parse("2026-01-01T00:00:01Z"),
            Instant.parse("2026-01-01T00:00:05Z"),
            0,
            2);
    assertEquals(3L, result.get("totalElements"));
    assertEquals(2L, result.get("totalPages"));
    var content = (List<AuditLog>) result.get("content");
    assertEquals(5, content.getFirst().durationMs());
    assertEquals(
        1, ((List<?>) store.logs("inventory", "ERROR", null, null, 1, 2).get("content")).size());
    assertThrows(ServiceException.class, () -> store.logs("bad", null, null, null, 0, 20));
    assertThrows(ServiceException.class, () -> store.logs(null, "bad", null, null, 0, 20));
    assertThrows(
        ServiceException.class, () -> store.logs(null, null, Instant.now(), Instant.EPOCH, 0, 20));
    assertThrows(ServiceException.class, () -> store.logs(null, null, null, null, -1, 20));
    assertThrows(ServiceException.class, () -> store.logs(null, null, null, null, 0, 101));
  }

  @Test
  void coherentSummaryRecentTwentyForEachServiceAndThreshold() {
    for (String service : List.of("inventory", "orders", "payments"))
      for (int i = 0; i < 40; i++) save(service, i < 6 ? "ERROR" : "SUCCESS", i);
    var summary = store.summary();
    for (var s : summary) {
      assertEquals(40L, s.get("totalCalls"));
      assertEquals(34L, s.get("successCount"));
      assertEquals(6L, s.get("errorCount"));
      assertEquals(85.0, s.get("successRate"));
      assertEquals(15.0, s.get("errorRate"));
      assertEquals(19.5, ((Number) s.get("averageDurationMs")).doubleValue());
      var recent = (List<AuditLog>) s.get("recentCalls");
      assertEquals(20, recent.size());
      assertEquals(20.0, recent.getFirst().durationMs());
      assertEquals(39.0, recent.getLast().durationMs());
    }
    save("inventory", "ERROR", 41);
    assertTrue(((Number) store.summary().getFirst().get("errorRate")).doubleValue() > 15);
  }

  @Test
  void duplicateIdIsExplicitAuditFailure() {
    var log =
        new AuditLog(
            "inventory",
            "getStock",
            1,
            "SUCCESS",
            UUID.randomUUID().toString(),
            Instant.now(),
            List.of(),
            Map.of(),
            null,
            null,
            null);
    store.save(log);
    assertEquals(
        "AUDIT_PERSISTENCE_FAILED",
        assertThrows(ServiceException.class, () -> store.save(log)).code());
  }
}
