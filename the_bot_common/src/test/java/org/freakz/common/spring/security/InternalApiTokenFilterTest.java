package org.freakz.common.spring.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalApiTokenFilterTest {

  @Test
  void rejectsMissingAndWrongTokens() throws Exception {
    InternalApiTokenFilter filter = filter();

    MockHttpServletResponse missingResponse = new MockHttpServletResponse();
    filter.doFilter(request(null), missingResponse, (request, response) -> {
      throw new AssertionError("request must not reach the controller");
    });
    assertThat(missingResponse.getStatus()).isEqualTo(401);

    MockHttpServletResponse wrongResponse = new MockHttpServletResponse();
    filter.doFilter(request("wrong"), wrongResponse, (request, response) -> {
      throw new AssertionError("request must not reach the controller");
    });
    assertThat(wrongResponse.getStatus()).isEqualTo(401);
  }

  @Test
  void acceptsCorrectTokenAndSkipsExcludedPath() throws Exception {
    InternalApiTokenFilter filter = filter();
    AtomicBoolean reached = new AtomicBoolean();

    MockHttpServletResponse acceptedResponse = new MockHttpServletResponse();
    filter.doFilter(request("secret"), acceptedResponse, (request, response) -> reached.set(true));
    assertThat(acceptedResponse.getStatus()).isEqualTo(200);
    assertThat(reached).isTrue();

    MockHttpServletRequest excludedRequest = request("wrong");
    excludedRequest.setRequestURI("/api/hokan/engine/openclaw/logs/read");
    AtomicBoolean excludedReached = new AtomicBoolean();
    filter.doFilter(excludedRequest, new MockHttpServletResponse(), (request, response) -> excludedReached.set(true));
    assertThat(excludedReached).isTrue();
  }

  private InternalApiTokenFilter filter() {
    return new InternalApiTokenFilter(
        "secret",
        List.of("/api/hokan/engine/"),
        List.of("/api/hokan/engine/openclaw/"));
  }

  private MockHttpServletRequest request(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/hokan/engine/internal/config/reload");
    if (token != null) {
      request.addHeader("X-TheBot-Internal-Token", token);
    }
    return request;
  }
}
