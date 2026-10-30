package edu.monitor.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    // Only internal services accept a propagated ID. The public gateway always creates one.
    String supplied =
        req.getRequestURI().startsWith("/internal/") ? req.getHeader("X-Request-Id") : null;
    String id;
    try {
      id = UUID.fromString(supplied).toString();
    } catch (Exception ignored) {
      id = UUID.randomUUID().toString();
    }
    MDC.put("requestId", id);
    res.setHeader("X-Request-Id", id);
    try {
      if (req.getContentLengthLong() > 32768) {
        res.setStatus(413);
        res.setContentType("application/json");
        res.getWriter()
            .write(
                "{\"requestId\":\""
                    + id
                    + "\",\"errorCode\":\"BODY_TOO_LARGE\",\"errorMessage\":\"El cuerpo excede 32"
                    + " KiB.\"}");
        return;
      }
      // Buffering bounds chunked bodies as well as requests with Content-Length.
      if ("POST".equals(req.getMethod())) {
        byte[] body = req.getInputStream().readNBytes(32769);
        if (body.length > 32768) {
          res.setStatus(413);
          res.setContentType("application/json");
          res.getWriter()
              .write(
                  "{\"requestId\":\""
                      + id
                      + "\",\"errorCode\":\"BODY_TOO_LARGE\",\"errorMessage\":\"El cuerpo excede 32"
                      + " KiB.\"}");
          return;
        }
        chain.doFilter(
            new HttpServletRequestWrapper(req) {
              @Override
              public ServletInputStream getInputStream() {
                var stream = new java.io.ByteArrayInputStream(body);
                return new ServletInputStream() {
                  public int read() {
                    return stream.read();
                  }

                  public boolean isFinished() {
                    return stream.available() == 0;
                  }

                  public boolean isReady() {
                    return true;
                  }

                  public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                  }
                };
              }
            },
            res);
      } else chain.doFilter(req, res);
    } finally {
      MDC.remove("requestId");
    }
  }
}
