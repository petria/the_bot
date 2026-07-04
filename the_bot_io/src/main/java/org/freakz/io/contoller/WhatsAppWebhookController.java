package org.freakz.io.contoller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.freakz.common.model.botconfig.WhatsAppConfig;
import org.freakz.io.config.ConfigService;
import org.freakz.io.connections.ConnectionManager;
import org.freakz.io.connections.WacliWebhookMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/connections/whatsapp")
public class WhatsAppWebhookController {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

  private final ConnectionManager connectionManager;
  private final ConfigService configService;
  private final ObjectMapper objectMapper;

  public WhatsAppWebhookController(ConnectionManager connectionManager, ConfigService configService, ObjectMapper objectMapper) {
    this.connectionManager = connectionManager;
    this.configService = configService;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/webhook")
  public ResponseEntity<?> webhook(
      @RequestBody String body,
      @RequestHeader(value = "X-Wacli-Signature", required = false) String signature) {
    try {
      verifySignature(body, signature);
      JsonNode payload = objectMapper.readTree(body);
      WacliWebhookMessageEvent event = WacliWebhookMessageEvent.from(payload);
      if (log.isDebugEnabled()) {
        log.debug("WhatsApp webhook parsed messageId={} chatJid={} senderJid={} text={} media={} directMediaUrl={} mediaContentType={} fields={}",
            event.getMessageId(),
            event.getChatJid(),
            event.effectiveSenderJid(),
            event.getText() != null && !event.getText().isBlank(),
            event.hasMedia(),
            event.hasDownloadableMediaUrl(),
            event.getMediaContentType(),
            WacliWebhookMessageEvent.fieldSummary(payload));
      }
      if (event.hasMedia() && !event.hasDownloadableMediaUrl()) {
        log.debug("WhatsApp webhook media will be downloaded through sidecar messageId={} chatJid={} directPath={} contentType={}",
            event.getMessageId(),
            event.getChatJid(),
            event.getMediaDirectPath(),
            event.getMediaContentType());
      } else if (!event.hasMedia() && looksLikeMediaPayload(payload)) {
        log.info("WhatsApp webhook contains media-like fields but no downloadable media URL was parsed messageId={} chatJid={} fields={}",
            event.getMessageId(),
            event.getChatJid(),
            WacliWebhookMessageEvent.fieldSummary(payload));
      }
      connectionManager.handleWhatsAppWebhook(event);
      return ResponseEntity.accepted().build();
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Unable to handle WhatsApp webhook", e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid WhatsApp webhook payload");
    }
  }

  private void verifySignature(String body, String signature) throws Exception {
    String secret = webhookSecret();
    if (secret == null || secret.isBlank()) {
      return;
    }
    if (signature == null || signature.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing WhatsApp webhook signature");
    }
    String expected = "sha256=" + hmacSha256(secret, body);
    if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid WhatsApp webhook signature");
    }
  }

  private String webhookSecret() throws Exception {
    WhatsAppConfig config = configService.readBotConfig().getWhatsappConfig();
    return config == null ? null : config.getWebhookSecret();
  }

  private String hmacSha256(String secret, String body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
  }

  private boolean looksLikeMediaPayload(JsonNode node) {
    if (node == null || node.isNull()) {
      return false;
    }
    String summary = WacliWebhookMessageEvent.fieldSummary(node).toLowerCase();
    return summary.contains("image")
        || summary.contains("video")
        || summary.contains("document")
        || summary.contains("sticker")
        || summary.contains("media")
        || summary.contains("mimetype")
        || summary.contains("mime");
  }
}
