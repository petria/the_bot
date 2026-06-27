package org.freakz.web.controller;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.engine.console.ConsoleCommandRequest;
import org.freakz.common.model.engine.console.ConsoleEvent;
import org.freakz.common.model.engine.console.ConsoleEventsResponse;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsoleControllerTest {

  @Test
  void commandForwardsLoggedInUserAndConsoleSessionToEngine() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.handleEngineRequest(any()))
        .thenReturn(ResponseEntity.ok(EngineResponse.builder().message("pong").build()));
    ConsoleController controller = new ConsoleController(engineClient);
    BotUserPrincipal principal = principal("petria");

    var response = controller.command(principal, new ConsoleCommandRequest("session-1", "!ping"));

    assertThat(response.accepted()).isTrue();
    assertThat(response.requestId()).isPositive();

    ArgumentCaptor<EngineRequest> captor = ArgumentCaptor.forClass(EngineRequest.class);
    verify(engineClient).handleEngineRequest(captor.capture());
    EngineRequest request = captor.getValue();
    assertThat(request.getNetwork()).isEqualTo("BOT_WEB_CONSOLE");
    assertThat(request.getFromSender()).isEqualTo("petria");
    assertThat(request.getFromSenderId()).isEqualTo("WEB-CONSOLE:7");
    assertThat(request.getCommand()).isEqualTo("!ping");
    assertThat(request.getChatProtocol()).isEqualTo("web");
    assertThat(request.getChatType()).isEqualTo("console");
    assertThat(request.getChatId()).isEqualTo("web-console:7:session-1");
    assertThat(request.getReplyTo()).isEqualTo("web-console:7:session-1");
  }

  @Test
  void eventsUsesAuthenticatedUserScopedSessionKey() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    ConsoleEventsResponse events = new ConsoleEventsResponse(List.of(new ConsoleEvent(1, 10, 20, "pong")));
    when(engineClient.getConsoleEvents("web-console:7:session-1", 0)).thenReturn(ResponseEntity.ok(events));
    ConsoleController controller = new ConsoleController(engineClient);

    ConsoleEventsResponse response = controller.events(principal("petria"), "session-1", 0);

    assertThat(response.events()).hasSize(1);
    verify(engineClient).getConsoleEvents("web-console:7:session-1", 0);
  }

  @Test
  void streamUsesAuthenticatedUserScopedSessionKey() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.consoleEventStreamUri("web-console:7:session-1", 5))
        .thenReturn(URI.create("http://bot-engine:8100/api/hokan/engine/internal/console/stream?sessionKey=web-console:7:session-1&afterId=5"));
    ConsoleController controller = new ConsoleController(engineClient);

    ResponseEntity<SseEmitter> response = controller.stream(principal("petria"), "session-1", 5);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/event-stream");
    assertThat(response.getHeaders().getFirst("X-Accel-Buffering")).isEqualTo("no");
    assertThat(response.getBody()).isNotNull();
    verify(engineClient).consoleEventStreamUri("web-console:7:session-1", 5);
  }

  @Test
  void commandRequiresCommandAndSession() {
    ConsoleController controller = new ConsoleController(mock(RestEngineClient.class));

    assertThatThrownBy(() -> controller.command(principal("petria"), new ConsoleCommandRequest("session-1", " ")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode.value")
        .isEqualTo(400);

    assertThatThrownBy(() -> controller.command(principal("petria"), new ConsoleCommandRequest(" ", "!ping")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode.value")
        .isEqualTo(400);
  }

  private BotUserPrincipal principal(String username) {
    User user = User.builder()
        .username(username)
        .password("$2a$10$abcdefghijklmnopqrstuv")
        .permissions(List.of("web_user"))
        .build();
    user.setId(7L);
    return BotUserPrincipal.from(user, List.of(new SimpleGrantedAuthority("web_user")));
  }
}
