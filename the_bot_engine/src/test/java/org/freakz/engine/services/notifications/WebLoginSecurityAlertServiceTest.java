package org.freakz.engine.services.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import org.freakz.common.model.security.WebLoginFailedEvent;
import org.junit.jupiter.api.Test;

class WebLoginSecurityAlertServiceTest {

  @Test
  void formatsFailedLoginAlertWithoutRawNewlinesOrQuotes() {
    WebLoginSecurityAlertService service = new WebLoginSecurityAlertService(mock(PrivateChatAlertService.class));
    WebLoginFailedEvent event = new WebLoginFailedEvent(
        "te\"st\nuser",
        "192.168.0.55\r\nbad",
        "Mozilla/5.0\tBrowser",
        Instant.parse("2026-05-20T17:30:00Z"));

    String alert = service.formatFailedLogin(event);

    assertThat(alert).contains("ALERT: failed web login");
    assertThat(alert).contains("username=\"te'st user\"");
    assertThat(alert).contains("from 192.168.0.55 bad");
    assertThat(alert).contains("userAgent=\"Mozilla/5.0 Browser\"");
    assertThat(alert).doesNotContain("\n").doesNotContain("\r").doesNotContain("\t");
  }
}
