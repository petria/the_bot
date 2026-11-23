package org.freakz.web.controller;

import java.time.Instant;

import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.freakz.common.model.mobile.NotificationSignature;
import org.freakz.web.mobile.MobileNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class InternalMobileNotificationControllerTest {

  @Test
  void onlyDeliversEventsWithAnIntactSignature() {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    var notifications = mock(MobileNotificationService.class);
    var controller = new InternalMobileNotificationController(notifications, mapper, "fixture-signing-key");
    var event = new MobileNotificationEvent("fixture", "example", "RULE_MATCH", "Title", "Body",
        "irc", "alias", null, Instant.now());
    byte[] body = mapper.writeValueAsBytes(event);
    String signature = NotificationSignature.sign("fixture-signing-key", body);
    controller.publish(body, signature);
    verify(notifications).accept(event);

    body[body.length - 1] ^= 1;
    assertThatThrownBy(() -> controller.publish(body, signature))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("401");
    verifyNoMoreInteractions(notifications);
  }
}
