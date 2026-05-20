package org.freakz.engine.services.notifications;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.freakz.common.model.security.WebLoginFailedEvent;
import org.springframework.stereotype.Service;

@Service
public class WebLoginSecurityAlertService {

  private static final int MAX_USERNAME_LENGTH = 80;
  private static final int MAX_ADDRESS_LENGTH = 80;
  private static final int MAX_USER_AGENT_LENGTH = 120;
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

  private final PrivateChatAlertService alertService;

  public WebLoginSecurityAlertService(PrivateChatAlertService alertService) {
    this.alertService = alertService;
  }

  public void notifyFailedLogin(WebLoginFailedEvent event) {
    alertService.sendAlertToConfiguredTargets(formatFailedLogin(event));
  }

  String formatFailedLogin(WebLoginFailedEvent event) {
    WebLoginFailedEvent safeEvent = event == null ? new WebLoginFailedEvent() : event;
    String message = String.format(
        "ALERT: failed web login username=\"%s\" from %s at %s",
        sanitize(safeEvent.getUsername(), MAX_USERNAME_LENGTH, "unknown"),
        sanitize(safeEvent.getRemoteAddress(), MAX_ADDRESS_LENGTH, "unknown"),
        safeEvent.getOccurredAt() == null ? "unknown time" : FORMATTER.format(safeEvent.getOccurredAt()));

    String userAgent = sanitize(safeEvent.getUserAgent(), MAX_USER_AGENT_LENGTH, "");
    if (!userAgent.isBlank()) {
      message += " userAgent=\"" + userAgent + "\"";
    }
    return message;
  }

  private String sanitize(String value, int maxLength, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
    normalized = normalized.replace('"', '\'');
    if (normalized.length() <= maxLength) {
      return normalized;
    }
    return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
  }
}
