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
import org.freakz.common.users.BotPermission;
import org.freakz.web.livechannels.LiveChannelAccessService;
import org.freakz.web.livechannels.LiveChannelCatalogService;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
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

    LiveChannelEventsResponse response = controller.events(principal("petria"), "IRC-HOKANDEV", 5);

    assertThat(response.events()).hasSize(1);
    verify(engineClient).getLiveChannelEvents("IRC-HOKANDEV", 5);
  }

  @Test
  void streamReturnsSseResponseForValidAlias() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.liveChannelEventStreamUri("IRC-HOKANDEV", 5))
        .thenReturn(URI.create("http://bot-engine:8100/api/hokan/engine/internal/live-channels/stream?echoToAlias=IRC-HOKANDEV&afterId=5"));
    AdminLiveChannelsController controller = controller(engineClient);

    ResponseEntity<SseEmitter> response = controller.stream(principal("petria"), "IRC-HOKANDEV", 5);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/event-stream");
    assertThat(response.getHeaders().getFirst("X-Accel-Buffering")).isEqualTo("no");
    assertThat(response.getBody()).isNotNull();
    verify(engineClient).liveChannelEventStreamUri("IRC-HOKANDEV", 5);
  }

  @Test
  void streamRequiresTargetAlias() {
    AdminLiveChannelsController controller = controller(mock(RestEngineClient.class));

    assertThatThrownBy(() -> controller.stream(principal("petria"), " ", 0))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode.value")
        .isEqualTo(400);
  }

  @Test
  void eventsRequiresViewPermission() {
    AdminLiveChannelsController controller = controller(mock(RestEngineClient.class));

    assertThatThrownBy(() -> controller.events(principal("viewer", BotPermission.WEB_USER), "IRC-HOKANDEV", 0))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode.value")
        .isEqualTo(403);
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
  void sendRequiresSendPermission() {
    AdminLiveChannelsController controller = controller(mock(RestEngineClient.class));

    assertThatThrownBy(() -> controller.send(
        principal("viewer", BotPermission.WEB_USER, BotPermission.LIVE_CHANNELS_VIEW_PREFIX + "irc-hokandev"),
        new LiveChannelSendRequest("IRC-HOKANDEV", null, "hello")))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode.value")
        .isEqualTo(403);
  }

  @Test
  void usersFetchesChannelUsersFromConnectionManager() {
    RestConnectionManagerClient connectionManagerClient = mock(RestConnectionManagerClient.class);
    ChannelUsersByEchoToAliasResponse users = new ChannelUsersByEchoToAliasResponse(
        List.of(ChannelUser.builder().nick("petria").account("petria").build()));
    when(connectionManagerClient.getChannelUsersByEchoToAlias(new ChannelUsersByEchoToAliasRequest("IRC-HOKANDEV")))
        .thenReturn(ResponseEntity.ok(users));
    AdminLiveChannelsController controller = new AdminLiveChannelsController(
        mock(RestEngineClient.class),
        connectionManagerClient,
        new LiveChannelAccessService(),
        mock(LiveChannelCatalogService.class));

    ChannelUsersByEchoToAliasResponse response = controller.users(principal("petria"), "IRC-HOKANDEV");

    assertThat(response.getChannelUsers()).hasSize(1);
    verify(connectionManagerClient).getChannelUsersByEchoToAlias(new ChannelUsersByEchoToAliasRequest("IRC-HOKANDEV"));
  }

  private AdminLiveChannelsController controller(RestEngineClient engineClient) {
    return new AdminLiveChannelsController(
        engineClient,
        mock(RestConnectionManagerClient.class),
        new LiveChannelAccessService(),
        mock(LiveChannelCatalogService.class));
  }

  private BotUserPrincipal principal(String username) {
    return principal(username, "web.admin");
  }

  private BotUserPrincipal principal(String username, String... permissions) {
    User user = User.builder()
        .username(username)
        .password("$2a$10$abcdefghijklmnopqrstuv")
        .permissions(List.of(permissions))
        .build();
    user.setId(7L);
    return BotUserPrincipal.from(user, List.of(permissions).stream()
        .map(SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList());
  }
}
