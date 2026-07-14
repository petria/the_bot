package org.freakz.engine.services;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.spring.rest.RestMessageSendClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ProcessingIndicatorServiceTest {

  private final RestMessageSendClient client = mock(RestMessageSendClient.class);
  private final ProcessingIndicatorService service = new ProcessingIndicatorService(client);

  @AfterEach
  void tearDown() {
    service.shutdown();
  }

  @Test
  void startsAndStopsWhatsAppIndicator() {
    EngineRequest request = request("whatsapp");

    service.start(request, "Hokan");
    service.stop(request);

    verify(client).sendProcessingIndicator(eq(7), any());
    verify(client).stopProcessingIndicator(eq(7), any());
  }

  @Test
  void refreshLifecycleSupportsTelegramAndDiscord() {
    service.start(request("telegram"), "Hokan");
    service.start(request("discord"), "Hokan");

    verify(client, org.mockito.Mockito.times(2)).sendProcessingIndicator(eq(7), any());
  }

  @Test
  void doesNotScheduleIndicatorsForIrc() {
    service.start(request("irc"), "Hokan");

    verifyNoInteractions(client);
  }

  private EngineRequest request(String protocol) {
    return EngineRequest.builder()
        .requestId(protocol + "-request")
        .fromConnectionId(7)
        .fromChannelId(42L)
        .replyTo("target")
        .chatProtocol(protocol)
        .timestamp(System.currentTimeMillis())
        .build();
  }
}
