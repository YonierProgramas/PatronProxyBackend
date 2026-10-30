package edu.monitor.audit;

import java.time.Instant;

public record AuditLog(
    String serviceId,
    String operation,
    double durationMs,
    String status,
    String requestId,
    Instant timestamp,
    Object inputParams,
    Object response,
    String errorCode,
    String errorMessage,
    String stackTraceSummary) {}
