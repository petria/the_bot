package org.freakz.web.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.spring.rest.RestBotWhatsappClient;
import org.freakz.common.spring.rest.RestEngineClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

class WhatsAppAuthServiceTest {

  private static final String QR_PAYLOAD = "2@a-very-long-whatsapp-qr-payload-for-tests,1234567890,abcdef";

  @TempDir
  Path tempDir;

  @Test
  void rendersQrPayloadToTokenizedMediaUrl() throws Exception {
    RestBotWhatsappClient whatsappClient = mock(RestBotWhatsappClient.class);
    RestEngineClient engineClient = mock(RestEngineClient.class);
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    MediaStorageSettingsResponse settings = new MediaStorageSettingsResponse(
        true, tempDir.toString(), "https://example.test/media", 25, 30, true, true, null);
    when(engineClient.getMediaStorageSettings()).thenReturn(ResponseEntity.ok(settings));
    when(whatsappClient.getAuthStatus()).thenReturn(ResponseEntity.ok(new RestBotWhatsappClient.AuthEnvelope(
        "OK",
        new RestBotWhatsappClient.AuthState(
            "WAITING_FOR_SCAN", false, false, true, "qr", QR_PAYLOAD,
            Instant.now().plusSeconds(120).toEpochMilli(), null, "Scan the QR code", null))));

    WhatsAppAuthService service = new WhatsAppAuthService(whatsappClient, engineClient, mapper);
    WhatsAppAuthService.WhatsAppAuthResponse response = service.getStatus();

    assertThat(response.state()).isEqualTo("WAITING_FOR_SCAN");
    assertThat(response.qrUrl()).startsWith("https://example.test/media/").contains("?token=");
    assertThat(response.qrUrl()).doesNotContain(Base64.getEncoder().encodeToString(QR_PAYLOAD.getBytes()));

    String id = response.qrUrl().substring(response.qrUrl().lastIndexOf("/", response.qrUrl().indexOf("?")) + 1,
        response.qrUrl().indexOf("?"));
    String token = response.qrUrl().substring(response.qrUrl().indexOf("token=") + "token=".length());
    java.nio.file.Path file = new org.freakz.common.media.MediaStore(tempDir, mapper)
        .readPublic(id, token)
        .orElseThrow()
        .file();
    BufferedImage image = ImageIO.read(file.toFile());
    Result decoded = new MultiFormatReader().decode(new BinaryBitmap(
        new HybridBinarizer(new BufferedImageLuminanceSource(image))));
    assertThat(decoded.getText()).isEqualTo(QR_PAYLOAD);
  }
}
