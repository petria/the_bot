package org.freakz.web.whatsapp;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import org.freakz.common.media.MediaStore;
import org.freakz.common.media.MediaStoreCreated;
import org.freakz.common.media.MediaStoreSource;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.spring.rest.RestBotWhatsappClient;
import org.freakz.common.spring.rest.RestEngineClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

@Service
public class WhatsAppAuthService {

  private static final Duration QR_TTL = Duration.ofMinutes(2);
  private static final int QR_SIZE = 512;
  private static final String QR_SOURCE = "whatsapp-auth";

  private final RestBotWhatsappClient whatsappClient;
  private final RestEngineClient engineClient;
  private final JsonMapper jsonMapper;

  private String qrHash;
  private MediaStoreCreated qrMedia;

  public WhatsAppAuthService(
      RestBotWhatsappClient whatsappClient,
      RestEngineClient engineClient,
      JsonMapper jsonMapper) {
    this.whatsappClient = whatsappClient;
    this.engineClient = engineClient;
    this.jsonMapper = jsonMapper;
  }

  public synchronized WhatsAppAuthResponse getStatus() {
    return toResponse(whatsappClient.getAuthStatus().getBody());
  }

  public synchronized WhatsAppAuthResponse start(String method, String phone) {
    if (!"qr".equalsIgnoreCase(method) && !"phone".equalsIgnoreCase(method)) {
      throw new IllegalArgumentException("Authentication method must be qr or phone");
    }
    removeQrMedia();
    return toResponse(whatsappClient.startAuth(method.toLowerCase(), phone).getBody());
  }

  public synchronized WhatsAppAuthResponse cancel() {
    WhatsAppAuthResponse response = toResponse(whatsappClient.cancelAuth().getBody());
    removeQrMedia();
    return response;
  }

  private WhatsAppAuthResponse toResponse(RestBotWhatsappClient.AuthEnvelope envelope) {
    if (envelope == null || envelope.auth() == null) {
      throw new IllegalStateException("WhatsApp sidecar returned no authentication state");
    }
    RestBotWhatsappClient.AuthState auth = envelope.auth();
    String qrUrl = null;
    Instant linkExpiresAt = null;
    if (auth.qrPayload() != null && !auth.qrPayload().isBlank()) {
      try {
        MediaLink link = ensureQrMedia(auth.qrPayload(), auth.qrExpiresAt());
        qrUrl = link.url();
        linkExpiresAt = link.expiresAt();
      } catch (Exception e) {
        removeQrMedia();
        return new WhatsAppAuthResponse(
            "MEDIA_ERROR",
            Boolean.TRUE.equals(auth.authenticated()),
            Boolean.TRUE.equals(auth.syncRunning()),
            Boolean.TRUE.equals(auth.authRunning()),
            auth.method(),
            null,
            null,
            auth.pairingCode(),
            "QR media is unavailable: " + e.getMessage(),
            "QR media could not be created");
      }
    } else if (Boolean.TRUE.equals(auth.authenticated()) || !Boolean.TRUE.equals(auth.authRunning())) {
      removeQrMedia();
    }
    return new WhatsAppAuthResponse(
        auth.state(),
        Boolean.TRUE.equals(auth.authenticated()),
        Boolean.TRUE.equals(auth.syncRunning()),
        Boolean.TRUE.equals(auth.authRunning()),
        auth.method(),
        qrUrl,
        linkExpiresAt,
        auth.pairingCode(),
        auth.message(),
        auth.error());
  }

  private MediaLink ensureQrMedia(String payload, Long sidecarExpiresAt) throws Exception {
    String nextHash = sha256(payload);
    if (!nextHash.equals(qrHash) || qrMedia == null) {
      removeQrMedia();
      MediaStorageSettingsResponse settings = mediaSettings();
      MediaStore store = mediaStore(settings);
      Instant now = Instant.now();
      Instant sidecarExpiry = sidecarExpiresAt == null
          ? now.plus(QR_TTL)
          : Instant.ofEpochMilli(sidecarExpiresAt);
      Duration ttl = Duration.between(now, sidecarExpiry);
      if (ttl.isNegative() || ttl.isZero()) {
        ttl = QR_TTL;
      }
      qrMedia = store.create(
          renderQr(payload),
          "image/png",
          "whatsapp-auth-qr.png",
          ttl.compareTo(QR_TTL) < 0 ? ttl : QR_TTL,
          new MediaStoreSource(QR_SOURCE, "WhatsApp", null, null, "admin"));
      qrHash = nextHash;
    }
    MediaStorageSettingsResponse settings = mediaSettings();
    String base = trimTrailingSlash(settings.publicUrlPrefix());
    return new MediaLink(
        base + "/" + qrMedia.id() + "?token=" + URLEncoder.encode(qrMedia.token(), StandardCharsets.UTF_8),
        qrMedia.expiresAt());
  }

  private MediaStorageSettingsResponse mediaSettings() {
    ResponseEntity<MediaStorageSettingsResponse> response = engineClient.getMediaStorageSettings();
    MediaStorageSettingsResponse settings = response.getBody();
    if (!response.getStatusCode().is2xxSuccessful() || settings == null) {
      throw new IllegalStateException("Could not load media storage settings");
    }
    if (!Boolean.TRUE.equals(settings.enabled())) {
      throw new IllegalStateException("Media storage is disabled");
    }
    if (settings.storageDir() == null || settings.storageDir().isBlank()) {
      throw new IllegalStateException("Media storage directory is not configured");
    }
    if (settings.publicUrlPrefix() == null || settings.publicUrlPrefix().isBlank()) {
      throw new IllegalStateException("Media public URL is not configured");
    }
    return settings;
  }

  private MediaStore mediaStore(MediaStorageSettingsResponse settings) {
    return new MediaStore(java.nio.file.Path.of(settings.storageDir()), jsonMapper);
  }

  private byte[] renderQr(String payload) throws Exception {
    BitMatrix matrix = new MultiFormatWriter().encode(
        payload,
        BarcodeFormat.QR_CODE,
        QR_SIZE,
        QR_SIZE,
        Map.of(EncodeHintType.MARGIN, 2, EncodeHintType.CHARACTER_SET, "UTF-8"));
    BufferedImage image = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB);
    for (int x = 0; x < QR_SIZE; x++) {
      for (int y = 0; y < QR_SIZE; y++) {
        image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
      }
    }
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "PNG", output)) {
      throw new IllegalStateException("PNG encoder is unavailable");
    }
    return output.toByteArray();
  }

  private void removeQrMedia() {
    if (qrMedia == null) {
      qrHash = null;
      return;
    }
    try {
      MediaStorageSettingsResponse settings = mediaSettings();
      mediaStore(settings).delete(qrMedia.id());
    } catch (Exception ignored) {
      // Expiry is the fallback if media settings are temporarily unavailable.
    }
    qrMedia = null;
    qrHash = null;
  }

  private String sha256(String value) throws Exception {
    return java.util.HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }

  private String trimTrailingSlash(String value) {
    return value == null ? "" : value.replaceFirst("/+$", "");
  }

  private record MediaLink(String url, Instant expiresAt) {
  }

  public record WhatsAppAuthResponse(
      String state,
      boolean authenticated,
      boolean syncRunning,
      boolean authRunning,
      String method,
      String qrUrl,
      Instant linkExpiresAt,
      String pairingCode,
      String message,
      String error) {
  }
}
