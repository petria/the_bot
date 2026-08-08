package org.freakz.engine.services.irc;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcNetwork;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.connectionmanager.IrcTopicEventRequest;
import org.freakz.common.model.connectionmanager.IrcTopicEventResponse;
import org.freakz.common.model.connectionmanager.IrcTopicSetResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IrcTopicManagementServiceTest {

  private ConfigService configService;
  private UsersService usersService;
  private RestConnectionManagerClient connectionManagerClient;
  private Channel channel;
  private IrcTopicManagementService service;

  @BeforeEach
  void setUp() {
    configService = mock(ConfigService.class);
    usersService = mock(UsersService.class);
    connectionManagerClient = mock(RestConnectionManagerClient.class);
    channel = Channel.builder()
        .name("#test")
        .echoToAlias("IRC-TEST")
        .manageTopic(true)
        .topic("saved topic")
        .build();
    when(configService.readBotConfig()).thenReturn(TheBotConfig.builder()
        .ircServerConfigs(List.of(IrcServerConfig.builder()
            .name("server")
            .ircNetwork(IrcNetwork.builder().name("IRCNet").build())
            .channelList(List.of(channel))
            .build()))
        .build());
    when(usersService.findAll()).thenReturn((List) List.of(user("petria", "petria", "channel.admin.irc.irc-test")));
    service = new IrcTopicManagementService(configService, usersService, connectionManagerClient);
  }

  @Test
  void authorizedTopicChangeIsAcceptedAndPersisted() throws Exception {
    IrcTopicEventResponse response = service.handleTopicEvent(
        new IrcTopicEventRequest("IRC-TEST", "#test", "new topic", "petria", true));

    assertThat(response.action()).isEqualTo("ACCEPT");
    assertThat(response.topic()).isEqualTo("new topic");
    verify(configService).updateIrcChannelTopic("IRC-TEST", "new topic");
  }

  @Test
  void wildcardPermissionAcceptsMatchingExternalIrcNick() throws Exception {
    when(usersService.findAll()).thenReturn((List) List.of(user("petria", "_Pete_", "*")));

    IrcTopicEventResponse response = service.handleTopicEvent(
        new IrcTopicEventRequest("IRC-TEST", "#test", "new topic", "_Pete_", true));

    assertThat(response.action()).isEqualTo("ACCEPT");
    verify(configService).updateIrcChannelTopic("IRC-TEST", "new topic");
  }

  @Test
  void unknownSetterIsRestoredAndNotPersisted() throws Exception {
    IrcTopicEventResponse response = service.handleTopicEvent(
        new IrcTopicEventRequest("IRC-TEST", "#test", "intruder topic", null, true));

    assertThat(response.action()).isEqualTo("RESTORE");
    assertThat(response.topic()).isEqualTo("saved topic");
    verify(configService, never()).updateIrcChannelTopic(anyString(), anyString());
  }

  @Test
  void topicSetSilentlyDeniesUserWithoutPermission() throws Exception {
    when(usersService.findAll()).thenReturn((List) List.of(user("other", "other", "channels.view.irc")));

    IrcTopicSetResponse response = service.setTopic("IRC-TEST", "new", request("other"));

    assertThat(response.changed()).isFalse();
    assertThat(response.error()).isNull();
    verifyNoInteractions(connectionManagerClient);
  }

  @Test
  void topicSetTruncatesLongTopicAndReportsIt() throws Exception {
    when(connectionManagerClient.setIrcTopic(any())).thenReturn(
        new IrcTopicSetResponse("IRC-TEST", "#test", true, true, "x".repeat(390), null));

    IrcTopicSetResponse response = service.setTopic("IRC-TEST", "x".repeat(500), request());

    assertThat(response.truncated()).isTrue();
    assertThat(response.topic()).hasSize(IrcTopicManagementService.FALLBACK_TOPIC_LIMIT);
  }

  @Test
  void webTopicAuthorizationUsesConfiguredChannelAdminPermission() {
    assertThat(service.canSetTopic("IRC-TEST", user("petria", "petria", "channel.admin.irc.irc-test")))
        .isTrue();
    assertThat(service.canSetTopic("IRC-TEST", user("viewer", "viewer", "channels.view.irc")))
        .isFalse();
    assertThat(service.findUser("PETRIA").getUsername()).isEqualTo("petria");
  }

  private EngineRequest request() {
    return EngineRequest.builder().user(user("petria", "petria", "channel.admin.irc.irc-test")).build();
  }

  private EngineRequest request(String username) {
    return EngineRequest.builder().user(user(username, username, "channels.view.irc")).build();
  }

  private User user(String username, String nick, String permission) {
    return User.builder().username(username).ircNick(nick).permissions(List.of(permission)).build();
  }
}
