package org.freakz.web.controller;

import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.freakz.web.config.TheBotWebProperties;
import org.freakz.web.mobile.MobileNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/mobile")
public class InternalMobileNotificationController {
  private final TheBotWebProperties properties;
  private final MobileNotificationService notifications;

  public InternalMobileNotificationController(TheBotWebProperties properties, MobileNotificationService notifications) {
    this.properties = properties;
    this.notifications = notifications;
  }

  @PostMapping("/notifications")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void publish(@RequestHeader(value = "X-TheBot-Internal-Token", required = false) String token,
                      @RequestBody MobileNotificationEvent event) {
    String expected = properties.getInternalApiToken();
    if (expected == null || expected.isBlank() || !expected.equals(token)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal token");
    }
    notifications.accept(event);
  }
}
