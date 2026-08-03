package org.freakz.web.controller;

import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.freakz.web.mobile.MobileNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/mobile")
public class InternalMobileNotificationController {
  private final MobileNotificationService notifications;

  public InternalMobileNotificationController(MobileNotificationService notifications) {
    this.notifications = notifications;
  }

  @PostMapping("/notifications")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void publish(@RequestBody MobileNotificationEvent event) {
    notifications.accept(event);
  }
}
