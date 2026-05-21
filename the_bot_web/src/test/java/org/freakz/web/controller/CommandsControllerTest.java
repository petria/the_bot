package org.freakz.web.controller;

import org.freakz.common.model.engine.commands.GetCommandsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.config.TheBotWebProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandsControllerTest {

  @Test
  void proxiesCommandsFromBotEngine() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    CommandsController controller = new CommandsController(engineClient, new TheBotWebProperties());
    GetCommandsResponse response = new GetCommandsResponse(List.of());
    when(engineClient.getCommands()).thenReturn(ResponseEntity.ok(response));

    ResponseEntity<?> entity = controller.getCommands();

    assertThat(entity.getBody()).isSameAs(response);
  }

  @Test
  void returnsBadGatewayWhenBotEngineIsUnavailable() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    TheBotWebProperties properties = new TheBotWebProperties();
    properties.setBotEngineBaseUrl("http://bot-engine:8100");
    CommandsController controller = new CommandsController(engineClient, properties);
    when(engineClient.getCommands()).thenThrow(new RestClientException("connect failed"));

    ResponseEntity<?> entity = controller.getCommands();

    assertThat(entity.getStatusCode().value()).isEqualTo(502);
    assertThat(entity.getBody())
        .extracting("code", "message", "botEngineBaseUrl", "detail")
        .containsExactly(
            "BOT_ENGINE_UNAVAILABLE",
            "Could not load commands from bot-engine",
            "http://bot-engine:8100",
            "connect failed");
  }
}
