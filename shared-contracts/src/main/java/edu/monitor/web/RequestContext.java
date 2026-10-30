package edu.monitor.web;

import org.slf4j.MDC;

public final class RequestContext {
  private RequestContext() {}

  public static String id() {
    return MDC.get("requestId");
  }
}
