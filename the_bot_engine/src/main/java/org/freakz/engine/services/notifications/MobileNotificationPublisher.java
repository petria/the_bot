package org.freakz.engine.services.notifications;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestBotWebSystemClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** Publishes user-scoped events to bot-web without coupling engine to mobile storage. */
@Service
public class MobileNotificationPublisher {

  private static final Logger log = LoggerFactory.getLogger(MobileNotificationPublisher.class);
  private final RestBotWebSystemClient botWebClient;

  public MobileNotificationPublisher(RestBotWebSystemClient botWebClient) {
    this.botWebClient = botWebClient;
  }

  public void publishReply(EngineRequest request, String reply) {
    if (request == null || reply == null || reply.isBlank() || isSynthetic(request)) {
      return;
    }
    User user = request.getUser();
    if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
      return;
    }
    publish(new MobileNotificationEvent(
        UUID.randomUUID().toString(),
        user.getUsername(),
        "BOT_REPLY",
        "Bot reply",
        reply,
        request.getChatProtocol(),
        request.getEchoToAlias(),
        request.getCommand(),
        Instant.now()));
  }

  public void publishRuleMatch(String username, String title, String body, EngineRequest request) {
    if (username == null || username.isBlank() || body == null || body.isBlank()) {
      return;
    }
    publish(new MobileNotificationEvent(
        UUID.randomUUID().toString(),
        username,
        "RULE_MATCH",
        title == null || title.isBlank() ? "Notification rule matched" : title,
        body,
        request == null ? null : request.getChatProtocol(),
        request == null ? null : request.getEchoToAlias(),
        null,
        Instant.now()));
  }

  private void publish(MobileNotificationEvent event) {
    try {
      botWebClient.publishMobileNotification(event);
    } catch (RuntimeException e) {
      log.debug("Mobile notification delivery unavailable for {}: {}", event.username(), e.getMessage());
    }
  }

  private boolean isSynthetic(EngineRequest request) {
    return "BOT_CLI_CLIENT".equals(request.getNetwork())
        || "BOT_INTERNAL".equals(request.getNetwork())
        || "BOT_MOBILE_CLIENT".equals(request.getNetwork())
        || "WEB_CLI".equals(request.getNetwork());
  }
}
