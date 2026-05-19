package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.BotConnectionChannelResponse;
import org.freakz.common.model.connectionmanager.BotConnectionResponse;
import org.freakz.common.model.connectionmanager.GetChannelActivityResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.web.config.AdminConnectionConfigService;
import org.freakz.web.config.TheBotWebProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

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
        new TheBotWebProperties());
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

    ResponseEntity<?> entity = controller.getConnectionMap();

    assertThat(entity.getBody()).isSameAs(response);
    assertThat(deletedChannel.isConfigured()).isFalse();
    assertThat(configuredChannel.isConfigured()).isTrue();
  }

  @Test
  void activityEndpointStillProxiesBotIoActivity() {
    RestConnectionManagerClient connectionManagerClient = mock(RestConnectionManagerClient.class);
    ConnectionsController controller = new ConnectionsController(
        connectionManagerClient,
        mock(AdminConnectionConfigService.class),
        new TheBotWebProperties());
    GetChannelActivityResponse response = new GetChannelActivityResponse(List.of());
    when(connectionManagerClient.getChannelActivityRequired()).thenReturn(response);

    ResponseEntity<?> entity = controller.getChannelActivity();

    assertThat(entity.getBody()).isSameAs(response);
  }
}
