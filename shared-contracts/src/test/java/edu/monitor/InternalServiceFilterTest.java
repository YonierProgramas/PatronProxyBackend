package edu.monitor;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.monitor.web.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;

class InternalServiceFilterTest {
  private static final String TOKEN = "test-only-internal-secret-at-least-32-characters";

  private InternalServiceFilter filter(String token, String profile) {
    var env = new MockEnvironment();
    env.setActiveProfiles(profile);
    return new InternalServiceFilter(token, env, new ObjectMapper());
  }

  @Test
  void validSecretDelegates() throws Exception {
    var request = new MockHttpServletRequest("POST", "/internal/getStock");
    request.addHeader(InternalServiceFilter.HEADER, TOKEN);
    var chain = new MockFilterChain();
    filter(TOKEN, "prod").doFilter(request, new MockHttpServletResponse(), chain);
    assertNotNull(chain.getRequest());
  }

  @Test
  void missingOrWrongSecretNeverReachesBusiness() throws Exception {
    for (String supplied : new String[] {"", "incorrect"}) {
      var request = new MockHttpServletRequest("POST", "/internal/getStock");
      if (!supplied.isEmpty()) request.addHeader(InternalServiceFilter.HEADER, supplied);
      var response = new MockHttpServletResponse();
      var chain = new MockFilterChain();
      filter(TOKEN, "prod").doFilter(request, response, chain);
      assertEquals(401, response.getStatus());
      assertNull(chain.getRequest());
      assertTrue(response.getContentAsString().contains("INTERNAL_UNAUTHORIZED"));
      assertFalse(response.getContentAsString().contains(TOKEN));
    }
  }

  @Test
  void normalizedPathCannotBypassProtection() throws Exception {
    var request = new MockHttpServletRequest("POST", "/internal;ignored/getStock");
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();
    filter(TOKEN, "prod").doFilter(request, response, chain);
    assertEquals(401, response.getStatus());
    assertNull(chain.getRequest());
  }

  @Test
  void productionRequiresStrongSecret() {
    assertThrows(IllegalStateException.class, () -> filter("", "prod"));
    assertThrows(IllegalStateException.class, () -> filter("short", "prod"));
  }

  @Test
  void healthAndGatewayRemainAccessible() throws Exception {
    for (String path :
        new String[] {
          "/actuator/health", "/api/metrics/summary", "/api/services/inventory/getStock"
        }) {
      var chain = new MockFilterChain();
      filter(TOKEN, "prod")
          .doFilter(new MockHttpServletRequest("GET", path), new MockHttpServletResponse(), chain);
      assertNotNull(chain.getRequest());
    }
  }

  @Test
  void localDevelopmentCanRunWithoutSecret() throws Exception {
    var chain = new MockFilterChain();
    filter("", "local")
        .doFilter(
            new MockHttpServletRequest("POST", "/internal/getStock"),
            new MockHttpServletResponse(),
            chain);
    assertNotNull(chain.getRequest());
  }

  @Test
  void deniedCallStillHasRequestId() throws Exception {
    var request = new MockHttpServletRequest("POST", "/internal/getStock");
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();
    new RequestFilter()
        .doFilter(request, response, (req, res) -> filter(TOKEN, "prod").doFilter(req, res, chain));
    assertNotNull(java.util.UUID.fromString(response.getHeader("X-Request-Id")));
    assertTrue(response.getContentAsString().contains(response.getHeader("X-Request-Id")));
  }
}
