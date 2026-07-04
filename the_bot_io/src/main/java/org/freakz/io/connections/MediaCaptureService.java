package org.freakz.io.connections;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.freakz.common.media.MediaStore;
import org.freakz.common.media.MediaStoreCreated;
import org.freakz.common.media.MediaStoreSource;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class MediaCaptureService {

  private static final Logger log = LoggerFactory.getLogger(MediaCaptureService.class);
  private static final long SETTINGS_CACHE_MILLIS = 60_000;

  private final RestEngineClient engineClient;
  private final JsonMapper jsonMapper;
  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  private volatile MediaStorageSettingsResponse cachedSettings;
  private volatile long cachedSettingsAt;

  public MediaCaptureService(RestEngineClient engineClient, JsonMapper jsonMapper) {
    this.engineClient = engineClient;
    this.jsonMapper = jsonMapper;
  }

  public void captureAndSend(
      ConnectionManager connectionManager,
      Channel sourceChannel,
      BotConnection connection,
      String sourceProtocol,
      String sender,
      String caption,
      String imageUrl,
      String contentType,
      String fileName) {
    if (connectionManager == null) {
      log.debug("Media capture skipped because connection manager is unavailable sourceProtocol={} sender={}", sourceProtocol, sender);
      return;
    }
    if (sourceChannel == null) {
      log.debug("Media capture skipped because source channel is not configured sourceProtocol={} sender={} imageUrl={}", sourceProtocol, sender, imageUrl);
      return;
    }
    if (!Boolean.TRUE.equals(sourceChannel.getCaptureImages())) {
      log.debug("Media capture skipped because capture is disabled sourceAlias={} sourceProtocol={}", sourceChannel.getEchoToAlias(), sourceProtocol);
      return;
    }
    List<String> targetAliases = sourceChannel.getCaptureImageToAliases();
    if (targetAliases == null || targetAliases.isEmpty()) {
      log.debug("Capture images enabled for {} but no capture targets configured", sourceChannel.getEchoToAlias());
      return;
    }
    try {
      MediaStorageSettingsResponse settings = mediaSettings();
      if (settings == null || !Boolean.TRUE.equals(settings.enabled())) {
        log.debug("Media capture skipped because media storage is disabled");
        return;
      }
      DownloadedMedia downloaded = download(imageUrl, settings.maxFileSizeMb() == null ? 25 : settings.maxFileSizeMb());
      String effectiveContentType = MediaStore.normalizeContentType(contentType, fileName);
      if (effectiveContentType == null) {
        effectiveContentType = MediaStore.normalizeContentType(downloaded.contentType(), fileName);
      }
      if (effectiveContentType == null) {
        log.debug("Media capture skipped unsupported image type url={} contentType={} fileName={}", imageUrl, contentType, fileName);
        return;
      }
      MediaStore store = new MediaStore(java.nio.file.Path.of(settings.storageDir()), jsonMapper);
      MediaStoreCreated created = store.create(
          downloaded.bytes(),
          effectiveContentType,
          fileName,
          Duration.ofDays(settings.retentionDays() == null ? 30 : settings.retentionDays()),
          new MediaStoreSource(
              sourceProtocol,
              connection == null ? null : connection.getNetwork(),
              sourceChannel.getEchoToAlias(),
              sourceChannel.getName(),
              sender));
      String publicUrl = trimTrailingSlash(settings.publicUrlPrefix()) + "/" + created.id() + "?token=" + created.token();
      String message = formatMessage(sender, caption, publicUrl);
      for (String alias : targetAliases) {
        if (alias == null || alias.isBlank()) {
          continue;
        }
        try {
          connectionManager.sendMessageByEchoToAlias(message, alias.trim());
        } catch (Exception e) {
          log.warn("Unable to send captured image link to {}: {}", alias, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Media capture failed for sourceAlias={} url={}: {}", sourceChannel.getEchoToAlias(), imageUrl, e.getMessage());
      log.debug("Media capture failure", e);
    }
  }

  private MediaStorageSettingsResponse mediaSettings() {
    long now = System.currentTimeMillis();
    MediaStorageSettingsResponse settings = cachedSettings;
    if (settings != null && now - cachedSettingsAt < SETTINGS_CACHE_MILLIS) {
      return settings;
    }
    settings = engineClient.getMediaStorageSettings().getBody();
    cachedSettings = settings;
    cachedSettingsAt = now;
    return settings;
  }

  private DownloadedMedia download(String imageUrl, int maxFileSizeMb) throws Exception {
    if (imageUrl == null || imageUrl.isBlank()) {
      throw new IllegalArgumentException("Missing image URL");
    }
    long maxBytes = Math.max(1, maxFileSizeMb) * 1024L * 1024L;
    HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
        .timeout(Duration.ofSeconds(30))
        .GET()
        .build();
    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("Image download failed: HTTP " + response.statusCode());
    }
    String responseContentType = response.headers().firstValue("content-type").orElse(null);
    String lengthHeader = response.headers().firstValue("content-length").orElse(null);
    if (lengthHeader != null) {
      try {
        if (Long.parseLong(lengthHeader) > maxBytes) {
          throw new IllegalArgumentException("Image is larger than configured media limit");
        }
      } catch (NumberFormatException ignored) {
      }
    }
    try (InputStream input = response.body(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      long total = 0;
      int read;
      while ((read = input.read(buffer)) != -1) {
        total += read;
        if (total > maxBytes) {
          throw new IllegalArgumentException("Image is larger than configured media limit");
        }
        output.write(buffer, 0, read);
      }
      return new DownloadedMedia(output.toByteArray(), responseContentType);
    }
  }

  private String formatMessage(String sender, String caption, String publicUrl) {
    StringBuilder message = new StringBuilder();
    if (sender != null && !sender.isBlank()) {
      message.append(sender.trim()).append(": ");
    }
    if (caption != null && !caption.isBlank()) {
      message.append(caption.trim()).append(" ");
    }
    message.append("[image: ").append(publicUrl).append("]");
    return message.toString();
  }

  private String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim().replaceFirst("/+$", "");
  }

  private record DownloadedMedia(byte[] bytes, String contentType) {
  }
}
