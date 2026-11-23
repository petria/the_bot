package org.freakz.io.controller;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.botconfig.WhatsAppConfig;
import org.freakz.io.config.ConfigService;
import org.freakz.io.connections.ConnectionManager;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WhatsAppWebhookControllerTest {

  @Test
  void requiresConfiguredSecretAndVerifiesOriginalBody() throws Exception {
    var manager = mock(ConnectionManager.class);
    var config = mock(ConfigService.class);
    var controller = new WhatsAppWebhookController(manager, config, new ObjectMapper(), "");
    when(config.readBotConfig()).thenReturn(TheBotConfig.builder().build());

    byte[] body = "{\"text\":\"Hyvää huomenta\"}\n".getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> controller.webhook(body, null))
        .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
    verifyNoInteractions(manager);

    when(config.readBotConfig()).thenReturn(TheBotConfig.builder()
        .whatsappConfig(WhatsAppConfig.builder().webhookSecret("test-key").build()).build());
    Mac hmac = Mac.getInstance("HmacSHA256");
    hmac.init(new SecretKeySpec("test-key".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String signature = "sha256=" + HexFormat.of().formatHex(hmac.doFinal(body));
    byte[] changed = Arrays.copyOf(body, body.length - 1);
    assertThatThrownBy(() -> controller.webhook(changed, signature))
        .isInstanceOf(ResponseStatusException.class).hasMessageContaining("401");
    verifyNoInteractions(manager);

    assertThat(controller.webhook(body, signature).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    verify(manager).handleWhatsAppWebhook(any());
  }
}
