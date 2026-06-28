package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.BotConnectionChannelResponse;
import org.freakz.common.model.connectionmanager.BotConnectionResponse;
import org.freakz.common.model.connectionmanager.ChannelActivityResponse;
import org.freakz.common.model.connectionmanager.GetChannelActivityResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.web.channels.ChannelAccessService;
import org.freakz.web.config.AdminConnectionConfigService;
import org.freakz.web.config.TheBotWebProperties;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectionsControllerTest {

  @Test
  void connectionMapUsesSavedConfigAsConfiguredSourceOfTruth() {
    RestConnectionManagerClient connectionManagerClient = mock(RestConnectionManagerClient.class);
    AdminConnectionConfigService configService = mock(AdminConnectionConfigService.class);
    ConnectionsController controller = new ConnectionsController(
        connectionManagerClient,
        configService,
        new TheBotWebProperties(),
        new ChannelAccessService());
    BotConnectionChannelResponse deletedChannel = new BotConnectionChannelResponse(
        "120363408176012025@g.us",
        "WHATSAPP_CONNECTION",
        "WhatsApp",
        "120363408176012025@g.us",
        "WHATSAPP-120363408176012025@g.us");
    deletedChannel.setConfigured(true);
    BotConnectionChannelResponse configuredChannel = new BotConnectionChannelResponse(
        "configured",
        "WHATSAPP_CONNECTION",
        "WhatsApp",
        "Configured",
        "WHATSAPP-CONFIGURED");
    configuredChannel.setConfigured(false);
    GetConnectionMapResponse response = new GetConnectionMapResponse(Map.of(
        1,
        new BotConnectionResponse(
            1,
            "WHATSAPP_CONNECTION",
            "WhatsApp",
            List.of(deletedChannel, configuredChannel))));
    when(connectionManagerClient.getConnectionMapRequired()).thenReturn(response);
    when(configService.hasConfiguredChannel("WHATSAPP_CONNECTION", "WhatsApp", "WHATSAPP-120363408176012025@g.us"))
        .thenReturn(false);
    when(configService.hasConfiguredChannel("WHATSAPP_CONNECTION", "WhatsApp", "WHATSAPP-CONFIGURED"))
        .thenReturn(true);

    ResponseEntity<?> entity = controller.getConnectionMap(principal("channels.view.whatsapp"));

    assertThat(entity.getBody()).isSameAs(response);
    assertThat(deletedChannel.isConfigured()).isFalse();
    assertThat(configuredChannel.isConfigured()).isTrue();
  }

  @Test
  void activityEndpointFiltersChannelsByViewPermission() {
    RestConnectionManagerClient connectionManagerClient = mock(RestConnectionManagerClient.class);
    ConnectionsController controller = new ConnectionsController(
        connectionManagerClient,
        mock(AdminConnectionConfigService.class),
        new TheBotWebProperties(),
        new ChannelAccessService());
    GetChannelActivityResponse response = new GetChannelActivityResponse(List.of(
        channelActivity("WHATSAPP_CONNECTION", "WHATSAPP-CONFIGURED"),
        channelActivity("IRC_CONNECTION", "IRC-AMIGAFIN")));
    when(connectionManagerClient.getChannelActivityRequired()).thenReturn(response);

    ResponseEntity<?> entity = controller.getChannelActivity(principal("channels.view.whatsapp"));

    GetChannelActivityResponse body = (GetChannelActivityResponse) entity.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getChannels())
        .extracting(ChannelActivityResponse::getEchoToAlias)
        .containsExactly("WHATSAPP-CONFIGURED");
  }

  private ChannelActivityResponse channelActivity(String type, String echoToAlias) {
    return ChannelActivityResponse.builder()
        .type(type)
        .echoToAlias(echoToAlias)
        .network("network")
        .name("name")
        .build();
  }

  private BotUserPrincipal principal(String... permissions) {
    User user = User.builder()
        .username("test")
        .password("hash")
        .permissions(List.of(permissions))
        .build();
    return BotUserPrincipal.from(user, List.of(permissions).stream()
        .map(SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList());
  }
}
