package edu.monitor.proxy;

import edu.monitor.audit.*;
import edu.monitor.contract.*;
import edu.monitor.web.RequestContext;
import java.time.Instant;
import java.util.*;
import org.slf4j.*;

public class LoggingProxy<T> implements MicroserviceProxy<T> {
  private static final Logger log = LoggerFactory.getLogger(LoggingProxy.class);
  private final String serviceId;
  private final MicroserviceProxy<T> delegate;
  private final AuditStore store;

  public LoggingProxy(String serviceId, MicroserviceProxy<T> delegate, AuditStore store) {
    this.serviceId = serviceId;
    this.delegate = delegate;
    this.store = store;
  }

  @Override
  public T execute(String operation, Object... params) {
    String requestId =
        Optional.ofNullable(RequestContext.id()).orElseGet(() -> UUID.randomUUID().toString());
    log.info(
        "Invocation started service={} operation={} requestId={}", serviceId, operation, requestId);
    long start = System.nanoTime();
    T result = null;
    ServiceException failure = null;
    try {
      result = delegate.execute(operation, params);
    } catch (ServiceException e) {
      failure = e;
    } catch (RuntimeException e) {
      failure = new ServiceException(502, "UPSTREAM_ERROR", "Error del servicio interno.", e);
    }
    double duration = (System.nanoTime() - start) / 1_000_000.0;
    String trace = null;
    if (failure != null) {
      trace =
          failure.getClass().getSimpleName()
              + ": "
              + failure.code()
              + " | "
              + Arrays.stream(failure.getStackTrace())
                  .limit(3)
                  .map(
                      frame ->
                          frame.getClassName()
                              + "."
                              + frame.getMethodName()
                              + ":"
                              + frame.getLineNumber())
                  .reduce((a, c) -> a + "; " + c)
                  .orElse("");
    }
    // Persist once, outside the delegation catch: audit failure must never retry business work.
    store.save(
        new AuditLog(
            serviceId,
            operation,
            duration,
            failure == null ? "SUCCESS" : "ERROR",
            requestId,
            Instant.now(),
            params,
            result,
            failure == null ? null : failure.code(),
            failure == null ? null : failure.getMessage(),
            trace));
    if (failure != null) throw failure;
    return result;
  }
}
