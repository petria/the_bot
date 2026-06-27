package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasResponse;
import org.freakz.common.model.engine.livechannel.LiveChannelEvent;
import org.freakz.common.model.engine.livechannel.LiveChannelEventsResponse;
import org.freakz.common.model.engine.livechannel.LiveChannelSendRequest;
import org.freakz.common.model.engine.livechannel.LiveChannelSendResponse;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminLiveChannelsControllerTest {

  @Test
  void eventsFetchesChannelEventsFromEngine() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    LiveChannelEventsResponse events = new LiveChannelEventsResponse(
        List.of(new LiveChannelEvent(1, 10, 20, "IRC-HOKANDEV", "petria", "id", "hello", "irc", "IRCNet", "channel", "chat", null)));
    when(engineClient.getLiveChannelEvents("IRC-HOKANDEV", 5)).thenReturn(ResponseEntity.ok(events));
    AdminLiveChannelsController controller = controller(engineClient);

    LiveChannelEventsResponse response = controller.events("IRC-HOKANDEV", 5);

    assertThat(response.events()).hasSize(1);
    verify(engineClient).getLiveChannelEvents("IRC-HOKANDEV", 5);
  }

  @Test
  void sendUsesAuthenticatedUsernameAndIgnoresRequestUsername() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.sendLiveChannelMessage(org.mockito.ArgumentMatchers.any()))
        .thenReturn(ResponseEntity.ok(new LiveChannelSendResponse(true, "IRC-HOKANDEV", "petria@web-ui>: hello")));
    AdminLiveChannelsController controller = controller(engineClient);

    LiveChannelSendResponse response = controller.send(
        principal("petria"),
        new LiveChannelSendRequest("IRC-HOKANDEV", "spoofed", "hello"));

    assertThat(response.sent()).isTrue();
    ArgumentCaptor<LiveChannelSendRequest> captor = ArgumentCaptor.forClass(LiveChannelSendRequest.class);
    verify(engineClient).sendLiveChannelMessage(captor.capture());
    assertThat(captor.getValue().echoToAlias()).isEqualTo("IRC-HOKANDEV");
    assertThat(captor.getValue().webUsername()).isEqualTo("petria");
    assertThat(captor.getValue().message()).isEqualTo("hello");
  }

  @Test
  void sendRequiresTargetAndMessage() {
    AdminLiveChannelsController controller = controller(mock(RestEngineClient.class));

    assertThatThrownBy(() -> controller.send(principal("petria"), new LiveChannelSendRequest(" ", null, "hello")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode.value")
        .isEqualTo(400);

    assertThatThrownBy(() -> controller.send(principal("petria"), new LiveChannelSendRequest("IRC-HOKANDEV", null, " ")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode.value")
        .isEqualTo(400);
  }

  @Test
  void usersFetchesChannelUsersFromConnectionManager() {
    RestConnectionManagerClient connectionManagerClient = mock(RestConnectionManagerClient.class);
    ChannelUsersByEchoToAliasResponse users = new ChannelUsersByEchoToAliasResponse(
        List.of(ChannelUser.builder().nick("petria").account("petria").build()));
    when(connectionManagerClient.getChannelUsersByEchoToAlias(new ChannelUsersByEchoToAliasRequest("IRC-HOKANDEV")))
        .thenReturn(ResponseEntity.ok(users));
    AdminLiveChannelsController controller = new AdminLiveChannelsController(mock(RestEngineClient.class), connectionManagerClient);

    ChannelUsersByEchoToAliasResponse response = controller.users("IRC-HOKANDEV");

    assertThat(response.getChannelUsers()).hasSize(1);
    verify(connectionManagerClient).getChannelUsersByEchoToAlias(new ChannelUsersByEchoToAliasRequest("IRC-HOKANDEV"));
  }

  private AdminLiveChannelsController controller(RestEngineClient engineClient) {
    return new AdminLiveChannelsController(engineClient, mock(RestConnectionManagerClient.class));
  }

  private BotUserPrincipal principal(String username) {
    User user = User.builder()
        .username(username)
        .password("$2a$10$abcdefghijklmnopqrstuv")
        .permissions(List.of("web.admin"))
        .build();
    user.setId(7L);
    return BotUserPrincipal.from(user, List.of(new SimpleGrantedAuthority("web.admin")));
  }
}
