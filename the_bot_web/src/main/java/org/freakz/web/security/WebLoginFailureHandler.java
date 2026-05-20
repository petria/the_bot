package org.freakz.web.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.freakz.common.model.security.WebLoginFailedEvent;
import org.freakz.common.spring.rest.RestEngineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class WebLoginFailureHandler implements AuthenticationFailureHandler {

  private static final Logger log = LoggerFactory.getLogger(WebLoginFailureHandler.class);
  private static final int MAX_USER_AGENT_LENGTH = 240;

  private final RestEngineClient restEngineClient;
  private final AuthenticationFailureHandler delegate =
      new SimpleUrlAuthenticationFailureHandler("/login?error");

  public WebLoginFailureHandler(RestEngineClient restEngineClient) {
    this.restEngineClient = restEngineClient;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {
    reportFailedLogin(request);
    delegate.onAuthenticationFailure(request, response, exception);
  }

  private void reportFailedLogin(HttpServletRequest request) {
    try {
      WebLoginFailedEvent event = new WebLoginFailedEvent(
          blankToUnknown(request.getParameter(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_FORM_USERNAME_KEY)),
          clientAddress(request),
          abbreviate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH),
          Instant.now());
      restEngineClient.reportWebLoginFailed(event);
    } catch (Exception e) {
      log.warn("Failed to report web login failure: {}", e.getMessage());
    }
  }

  private String clientAddress(HttpServletRequest request) {
    String forwardedFor = firstNonBlank(request.getHeader("X-Forwarded-For"));
    if (!forwardedFor.isBlank()) {
      return forwardedFor.split(",", 2)[0].trim();
    }
    return firstNonBlank(request.getHeader("X-Real-IP"), request.getRemoteAddr(), "unknown");
  }

  private String blankToUnknown(String value) {
    String normalized = firstNonBlank(value);
    return normalized.isBlank() ? "unknown" : normalized;
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return "";
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }

  private String abbreviate(String value, int maxLength) {
    String normalized = firstNonBlank(value).replaceAll("[\\r\\n]+", " ");
    if (normalized.length() <= maxLength) {
      return normalized;
    }
    return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
  }
}
