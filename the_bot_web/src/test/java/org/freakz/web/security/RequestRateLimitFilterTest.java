package org.freakz.web.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RequestRateLimitFilterTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC);

  @Test
  void limitsFailedLoginAttemptsAndPreservesRequestBodyForAuthentication() throws Exception {
    var filter = new RequestRateLimitFilter(Set.of(), mock(WebLoginFailureHandler.class), new JsonMapper(), CLOCK);
    byte[] body = "{\"username\":\"example\",\"password\":\"test\"}".getBytes(StandardCharsets.UTF_8);
    for (int attempt = 0; attempt < 6; attempt++) {
      var request = new MockHttpServletRequest("POST", "/api/mobile/auth/login");
      request.setRemoteAddr("192.0.2.1");
      request.addHeader("X-Real-IP", "192.0.2." + (10 + attempt));
      request.setContent(body);
      var response = new MockHttpServletResponse();
      filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        assertThat(servletRequest.getInputStream().readAllBytes()).isEqualTo(body);
        ((jakarta.servlet.http.HttpServletResponse) servletResponse).setStatus(401);
      });
      assertThat(response.getStatus()).isEqualTo(attempt < 5 ? 401 : 429);
      if (attempt == 5) assertThat(response.getHeader("Retry-After")).isEqualTo("900");
    }
  }

  @Test
  void enforcesMediaBudgetAndRecognizesTrustedForwarder() throws Exception {
    var filter = new RequestRateLimitFilter(Set.of("192.0.2.10"), mock(WebLoginFailureHandler.class), new JsonMapper(), CLOCK);
    for (int attempt = 0; attempt < 32; attempt++) {
      var request = new MockHttpServletRequest("GET", "/m/example");
      request.setRemoteAddr("192.0.2.10");
      request.addHeader("X-Real-IP", attempt == 31 ? "192.0.2.2" : "192.0.2.1");
      var response = new MockHttpServletResponse();
      filter.doFilter(request, response, (req, res) -> {});
      assertThat(response.getStatus()).isEqualTo(attempt == 30 ? 429 : 200);
    }
  }
}
