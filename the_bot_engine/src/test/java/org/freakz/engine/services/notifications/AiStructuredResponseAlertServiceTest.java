package org.freakz.engine.services.notifications;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.services.ai.hermes.HermesSettings;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AiStructuredResponseAlertServiceTest {

  @Test
  void formatsSanitizedStructuredResponseAlert() {
    AiStructuredResponseAlertService service =
        new AiStructuredResponseAlertService(mock(PrivateChatAlertService.class), fixedClock());
    EngineRequest request = EngineRequest.builder()
        .command("!weather turku")
        .chatProtocol("irc")
        .network("IRCNet")
        .replyTo("#lowlife")
        .fromSender("_Pete_\nraw")
        .fromSenderId("pete-id")
        .build();

    String alert = service.formatAlert(
        "ai-command",
        "!weather",
        request,
        new HermesSettings("http://hermes", "", "qwen\nbad", 120, "chat-completions"));

    assertThat(alert)
        .isEqualTo("ALERT: AI structured response rejected source=ai-command command=!weather protocol=irc network=IRCNet target=#lowlife sender=_Pete_ raw model=qwen bad apiMode=chat-completions");
  }

  @Test
  void rateLimitsDuplicateAlerts() {
    PrivateChatAlertService privateChatAlertService = mock(PrivateChatAlertService.class);
    AiStructuredResponseAlertService service =
        new AiStructuredResponseAlertService(privateChatAlertService, fixedClock());
    EngineRequest request = EngineRequest.builder()
        .command("!dynping")
        .chatProtocol("irc")
        .network("IRCNet")
        .replyTo("#lowlife")
        .fromSender("_Pete_")
        .build();
    HermesSettings settings = new HermesSettings("http://hermes", "", "model", 120, "responses");

    service.notifyRejected("ai-command", "!dynping", request, settings);
    service.notifyRejected("ai-command", "!dynping", request, settings);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(privateChatAlertService, times(1)).sendAlertToConfiguredTargets(captor.capture());
    assertThat(captor.getValue()).contains("source=ai-command", "command=!dynping", "target=#lowlife");
  }

  private Clock fixedClock() {
    return Clock.fixed(Instant.parse("2026-06-17T08:00:00Z"), ZoneOffset.UTC);
  }
}
