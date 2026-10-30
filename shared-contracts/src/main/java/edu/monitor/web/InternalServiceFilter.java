package edu.monitor.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.monitor.contract.ApiError;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InternalServiceFilter extends OncePerRequestFilter {
  public static final String HEADER = "X-Internal-Service-Token";
  private final byte[] expected;
  private final ObjectMapper mapper;

  public InternalServiceFilter(
      @Value("${internal.service.token:}") String token,
      Environment environment,
      ObjectMapper mapper) {
    if ((environment.acceptsProfiles(Profiles.of("prod")) || !token.isEmpty())
        && token.length() < 32) {
      throw new IllegalStateException(
          "INTERNAL_SERVICE_TOKEN must contain at least 32 characters in production.");
    }
    this.expected = token.getBytes(StandardCharsets.UTF_8);
    this.mapper = mapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = new UrlPathHelper().getPathWithinApplication(request);
    return expected.length == 0 || !(path.equals("/internal") || path.startsWith("/internal/"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String supplied = request.getHeader(HEADER);
    if (supplied == null
        || !MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8))) {
      response.setStatus(401);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.setHeader("Cache-Control", "no-store");
      mapper.writeValue(
          response.getWriter(),
          new ApiError(
              RequestContext.id(),
              "INTERNAL_UNAUTHORIZED",
              "No se autorizó el acceso al servicio interno."));
      return;
    }
    // Do not log or return the internal credential. Health and public gateway routes stay
    // accessible.
    chain.doFilter(request, response);
  }
}
