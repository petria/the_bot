package org.freakz.engine.services.notifications;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.services.ai.hermes.HermesSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiStructuredResponseAlertService {

  private static final Duration ALERT_INTERVAL = Duration.ofMinutes(10);

  private final PrivateChatAlertService alertService;
  private final Clock clock;
  private final ConcurrentMap<String, Instant> lastSent = new ConcurrentHashMap<>();

  @Autowired
  public AiStructuredResponseAlertService(PrivateChatAlertService alertService) {
    this(alertService, Clock.systemDefaultZone());
  }

  AiStructuredResponseAlertService(PrivateChatAlertService alertService, Clock clock) {
    this.alertService = alertService;
    this.clock = clock;
  }

  public void notifyRejected(String source, String commandName, EngineRequest request, HermesSettings settings) {
    String key = dedupeKey(source, commandName, request, settings);
    Instant now = Instant.now(clock);
    Instant previous = lastSent.get(key);
    if (previous != null && previous.plus(ALERT_INTERVAL).isAfter(now)) {
      return;
    }
    lastSent.put(key, now);
    alertService.sendAlertToConfiguredTargets(formatAlert(source, commandName, request, settings));
  }

  String formatAlert(String source, String commandName, EngineRequest request, HermesSettings settings) {
    return "ALERT: AI structured response rejected"
        + " source=" + sanitize(firstNonBlank(source, "unknown"))
        + " command=" + sanitize(firstNonBlank(commandName, commandFromRequest(request), "?"))
        + " protocol=" + sanitize(firstNonBlank(protocol(request), "?"))
        + " network=" + sanitize(firstNonBlank(request == null ? null : request.getNetwork(), "?"))
        + " target=" + sanitize(firstNonBlank(target(request), "?"))
        + " sender=" + sanitize(firstNonBlank(request == null ? null : request.getFromSender(), request == null ? null : request.getFromSenderId(), "?"))
        + " model=" + sanitize(firstNonBlank(settings == null ? null : settings.model(), "?"))
        + " apiMode=" + sanitize(firstNonBlank(settings == null ? null : settings.apiMode(), "?"));
  }

  private String dedupeKey(String source, String commandName, EngineRequest request, HermesSettings settings) {
    return sanitize(firstNonBlank(source, "unknown"))
        + "|" + sanitize(firstNonBlank(commandName, commandFromRequest(request), "?"))
        + "|" + sanitize(firstNonBlank(protocol(request), "?"))
        + "|" + sanitize(firstNonBlank(request == null ? null : request.getNetwork(), "?"))
        + "|" + sanitize(firstNonBlank(target(request), "?"))
        + "|" + sanitize(firstNonBlank(settings == null ? null : settings.model(), "?"))
        + "|" + sanitize(firstNonBlank(settings == null ? null : settings.apiMode(), "?"));
  }

  private String commandFromRequest(EngineRequest request) {
    if (request == null || request.getCommand() == null || request.getCommand().isBlank()) {
      return null;
    }
    String[] parts = request.getCommand().trim().split("\\s+", 2);
    return parts.length == 0 ? null : parts[0];
  }

  private String protocol(EngineRequest request) {
    if (request == null) {
      return null;
    }
    return ChatIdentityUtil.sanitize(request.getChatProtocol(), ChatIdentityUtil.resolveProtocol(request.getNetwork()));
  }

  private String target(EngineRequest request) {
    if (request == null) {
      return null;
    }
    if (request.isPrivateChannel()) {
      return "private";
    }
    return firstNonBlank(request.getReplyTo(), ChatIdentityUtil.extractTargetFromChatId(request.getChatId(), null));
  }

  private String sanitize(String value) {
    if (value == null || value.isBlank()) {
      return "?";
    }
    String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim().replace('"', '\'');
    if (normalized.length() <= 120) {
      return normalized;
    }
    return normalized.substring(0, 117) + "...";
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }
}
