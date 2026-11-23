package org.freakz.web.controller;

import java.time.Duration;
import java.time.Instant;

import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.freakz.common.model.mobile.NotificationSignature;
import org.freakz.web.mobile.MobileNotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/internal/mobile")
public class InternalMobileNotificationController {

  private static final Duration EVENT_LIFETIME = Duration.ofMinutes(5);

  private final MobileNotificationService notifications;
  private final JsonMapper mapper;
  private final String signingKey;

  public InternalMobileNotificationController(MobileNotificationService notifications,
      @Qualifier("jsonMapper") JsonMapper mapper,
      @Value("${the.bot.notification.signing-key:${THE_BOT_NOTIFICATION_SIGNING_KEY:}}") String signingKey) {
    this.notifications = notifications;
    this.mapper = mapper;
    this.signingKey = signingKey;
  }

  @PostMapping("/notifications")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void publish(@RequestBody byte[] body,
      @RequestHeader(value = NotificationSignature.HEADER, required = false) String signature) {
    if (signingKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
    }
    if (!NotificationSignature.matches(signingKey, body, signature)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    MobileNotificationEvent event;
    try {
      event = mapper.readValue(body, MobileNotificationEvent.class);
    } catch (JacksonException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification event");
    }
    Instant now = Instant.now();
    if (event == null || event.eventId() == null || event.eventId().isBlank() || event.eventId().length() > 128
        || event.occurredAt() == null || event.occurredAt().isBefore(now.minus(EVENT_LIFETIME))
        || event.occurredAt().isAfter(now.plus(EVENT_LIFETIME))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification ID or timestamp");
    }
    notifications.accept(event);
  }
}
