package edu.monitor.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.monitor.contract.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class AuditStore {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;

  public AuditStore(JdbcTemplate jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  public void save(AuditLog log) {
    try {
      jdbc.update(
          "INSERT INTO"
              + " audit_log(service_id,operation,duration_ms,status,request_id,timestamp,input_params,response,error_code,error_message,stack_trace_summary)"
              + " VALUES (?,?,?,?,?,?,?,?,?,?,?)",
          log.serviceId(),
          log.operation(),
          log.durationMs(),
          log.status(),
          log.requestId(),
          OffsetDateTime.ofInstant(log.timestamp(), ZoneOffset.UTC),
          mapper.writeValueAsString(log.inputParams()),
          log.response() == null ? null : mapper.writeValueAsString(log.response()),
          log.errorCode(),
          log.errorMessage(),
          log.stackTraceSummary());
    } catch (Exception e) {
      throw new ServiceException(
          500,
          "AUDIT_PERSISTENCE_FAILED",
          "La operación terminó, pero no se pudo guardar la auditoría. No repitas automáticamente"
              + " la operación.",
          e);
    }
  }

  private final RowMapper<AuditLog> row =
      (rs, n) ->
          new AuditLog(
              rs.getString("service_id"),
              rs.getString("operation"),
              rs.getDouble("duration_ms"),
              rs.getString("status"),
              rs.getString("request_id"),
              rs.getObject("timestamp", OffsetDateTime.class).toInstant(),
              json(rs.getString("input_params")),
              json(rs.getString("response")),
              rs.getString("error_code"),
              rs.getString("error_message"),
              rs.getString("stack_trace_summary"));

  private Object json(String s) {
    try {
      return s == null ? null : mapper.readValue(s, Object.class);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid persisted JSON", e);
    }
  }

  public Map<String, Object> logs(
      String service, String status, Instant from, Instant to, int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw Inputs.invalid("Página o tamaño inválido (1–100).");
    if (service != null && !List.of("inventory", "orders", "payments").contains(service))
      throw Inputs.invalid("Servicio inválido.");
    if (status != null && !List.of("SUCCESS", "ERROR").contains(status))
      throw Inputs.invalid("Estado inválido.");
    if (from != null && to != null && from.isAfter(to))
      throw Inputs.invalid("El inicio debe ser anterior al fin.");
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    List<Object> args = new ArrayList<>();
    if (service != null) {
      where.append(" AND service_id=?");
      args.add(service);
    }
    if (status != null) {
      where.append(" AND status=?");
      args.add(status);
    }
    if (from != null) {
      where.append(" AND timestamp>=?");
      args.add(OffsetDateTime.ofInstant(from, ZoneOffset.UTC));
    }
    if (to != null) {
      where.append(" AND timestamp<=?");
      args.add(OffsetDateTime.ofInstant(to, ZoneOffset.UTC));
    }
    long count =
        jdbc.queryForObject("SELECT COUNT(*) FROM audit_log" + where, Long.class, args.toArray());
    args.add(size);
    args.add((long) page * size);
    var content =
        jdbc.query(
            "SELECT * FROM audit_log" + where + " ORDER BY timestamp DESC,id DESC LIMIT ? OFFSET ?",
            row,
            args.toArray());
    return Map.of(
        "content",
        content,
        "page",
        page,
        "size",
        size,
        "totalElements",
        count,
        "totalPages",
        (count + size - 1) / size);
  }

  public List<Map<String, Object>> summary() {
    // Two set-based queries, independent of service count; no per-row fetches.
    var totals =
        jdbc.queryForList(
            "SELECT service_id,COUNT(*) total_calls,SUM(CASE WHEN status='SUCCESS' THEN 1 ELSE 0"
                + " END) success_count,AVG(duration_ms) average_duration FROM audit_log GROUP BY"
                + " service_id");
    var recent =
        jdbc.query(
            "SELECT * FROM (SELECT a.*,ROW_NUMBER() OVER(PARTITION BY service_id ORDER BY timestamp"
                + " DESC,id DESC) rn FROM audit_log a) r WHERE rn<=20 ORDER BY timestamp ASC,id"
                + " ASC",
            row);
    List<Map<String, Object>> result = new ArrayList<>();
    for (String id : List.of("inventory", "orders", "payments")) {
      var total =
          totals.stream().filter(t -> id.equals(t.get("service_id"))).findFirst().orElse(Map.of());
      long calls = ((Number) total.getOrDefault("total_calls", 0)).longValue(),
          success = ((Number) total.getOrDefault("success_count", 0)).longValue();
      Map<String, Object> value = new LinkedHashMap<>();
      value.put("serviceId", id);
      value.put("totalCalls", calls);
      value.put("successCount", success);
      value.put("errorCount", calls - success);
      value.put("successRate", calls == 0 ? null : success * 100.0 / calls);
      value.put("errorRate", calls == 0 ? null : (calls - success) * 100.0 / calls);
      value.put("averageDurationMs", total.get("average_duration"));
      value.put("recentCalls", recent.stream().filter(l -> l.serviceId().equals(id)).toList());
      result.add(value);
    }
    return result;
  }
}
