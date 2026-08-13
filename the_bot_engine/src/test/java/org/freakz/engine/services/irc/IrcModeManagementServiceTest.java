package org.freakz.engine.services.irc;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcNetwork;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.connectionmanager.IrcModeEventRequest;
import org.freakz.common.model.connectionmanager.IrcModeEventResponse;
import org.freakz.common.model.connectionmanager.IrcModeSetResponse;
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

class IrcModeManagementServiceTest {

  private ConfigService configService;
  private UsersService usersService;
  private RestConnectionManagerClient connectionManagerClient;
  private IrcModeManagementService service;

  @BeforeEach
  void setUp() {
    configService = mock(ConfigService.class);
    usersService = mock(UsersService.class);
    connectionManagerClient = mock(RestConnectionManagerClient.class);
    Channel channel = Channel.builder()
        .name("#test")
        .echoToAlias("IRC-TEST")
        .manageMode(true)
        .modes("+st")
        .build();
    when(configService.readBotConfig()).thenReturn(TheBotConfig.builder()
        .ircServerConfigs(List.of(IrcServerConfig.builder()
            .name("server")
            .ircNetwork(IrcNetwork.builder().name("IRCNet").build())
            .channelList(List.of(channel))
            .build()))
        .build());
    when(usersService.findAll()).thenReturn((List) List.of(user("petria", "petria", "channel.admin.irc.irc-test")));
    service = new IrcModeManagementService(configService, usersService, connectionManagerClient);
  }

  @Test
  void authorizedModeChangeIsAcceptedAndPersisted() throws Exception {
    IrcModeEventResponse response = service.handleModeEvent(
        new IrcModeEventRequest("IRC-TEST", "#test", "+nst", "petria"));

    assertThat(response.action()).isEqualTo("ACCEPT");
    assertThat(response.modes()).isEqualTo("+nst");
    verify(configService).updateIrcChannelModes("IRC-TEST", "+nst");
  }

  @Test
  void unauthorizedModeChangeIsRestored() throws Exception {
    IrcModeEventResponse response = service.handleModeEvent(
        new IrcModeEventRequest("IRC-TEST", "#test", "+n", "intruder"));

    assertThat(response.action()).isEqualTo("RESTORE");
    assertThat(response.modes()).isEqualTo("+st");
    verify(configService, never()).updateIrcChannelModes(anyString(), anyString());
  }

  @Test
  void emptyConfiguredModesCanBeSetByAuthorizedUser() throws Exception {
    when(connectionManagerClient.setIrcModes(any())).thenReturn(
        new IrcModeSetResponse("IRC-TEST", "#test", true, "", null));

    IrcModeSetResponse response = service.setModes("IRC-TEST", "", request());

    assertThat(response.modes()).isEmpty();
    verify(configService).updateIrcChannelModes("IRC-TEST", "");
  }

  private EngineRequest request() {
    return EngineRequest.builder().user(user("petria", "petria", "channel.admin.irc.irc-test")).build();
  }

  private User user(String username, String nick, String permission) {
    return User.builder().username(username).ircNick(nick).permissions(List.of(permission)).build();
  }
}
