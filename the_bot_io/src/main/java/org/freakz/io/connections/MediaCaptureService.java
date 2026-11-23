package org.freakz.io.connections;

import java.net.URI;
import org.freakz.common.net.PublicHttpFetcher;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.freakz.common.media.MediaStore;
import org.freakz.common.media.MediaStoreCreated;
import org.freakz.common.media.MediaStoreSource;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
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
    captureAndSend(connectionManager, sourceChannel, connection, sourceProtocol, sender, caption, imageUrl, contentType, fileName, Map.of());
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
      String fileName,
      Map<String, String> requestHeaders) {
    captureAndSend(connectionManager, sourceChannel, connection, sourceProtocol, sender, caption,
        imageUrl, contentType, fileName, requestHeaders, null);
  }

  public record ReplyContext(String channelId, String messageId, String threadId, String senderId) {
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
      String fileName,
      Map<String, String> requestHeaders,
      ReplyContext replyContext) {
    captureAndSend(connectionManager, sourceChannel, connection, sourceProtocol, sender, caption,
        imageUrl, contentType, fileName, requestHeaders, replyContext, false);
  }

  void captureAndSend(
      ConnectionManager connectionManager, Channel sourceChannel, BotConnection connection,
      String sourceProtocol, String sender, String caption, String imageUrl, String contentType, String fileName,
      Map<String, String> requestHeaders, ReplyContext replyContext, boolean configuredService) {
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
    boolean replyToSource = Boolean.TRUE.equals(sourceChannel.getShowCapturedUrl());
    TreeSet<String> targetAliases = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    List<String> configuredAliases = sourceChannel.getCaptureImageToAliases();
    if (configuredAliases != null) {
      configuredAliases.stream().filter(alias -> alias != null && !alias.isBlank())
          .map(String::trim).forEach(targetAliases::add);
    }
    if (targetAliases.isEmpty() && !replyToSource) {
      log.debug("Capture media enabled for {} but no capture targets configured", sourceChannel.getEchoToAlias());
      return;
    }
    try {
      MediaStorageSettingsResponse settings = mediaSettings();
      if (settings == null || !Boolean.TRUE.equals(settings.enabled())) {
        log.debug("Media capture skipped because media storage is disabled");
        return;
      }
      DownloadedMedia downloaded = download(imageUrl, settings.maxFileSizeMb() == null ? 25 : settings.maxFileSizeMb(), requestHeaders, configuredService);
      String effectiveContentType = MediaStore.normalizeContentType(contentType, fileName);
      if (effectiveContentType == null) {
        effectiveContentType = MediaStore.normalizeContentType(downloaded.contentType(), fileName);
      }
      if (effectiveContentType == null) {
        log.debug("Media capture skipped unsupported media type url={} contentType={} fileName={}", imageUrl, contentType, fileName);
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
      String publicUrl = publicMediaUrl(settings, created);
      if (replyToSource) {
        try {
          if (connection == null) {
            throw new IllegalStateException("Source connection is unavailable");
          }
          connection.sendMessageTo(capturedLinkMessage(connection, sourceChannel.getId(),
              sourceChannel.getName(), connectionManager.resolveCapturedMediaName(connection, sourceProtocol,
                  sender, replyContext == null ? null : replyContext.senderId()),
              MediaStore.mediaTypeLabel(effectiveContentType), publicUrl, replyContext));
          if (sourceChannel.getEchoToAlias() != null) {
            targetAliases.remove(sourceChannel.getEchoToAlias().trim());
          }
        } catch (Exception e) {
          log.warn("Unable to send captured media link to source {}: {}", sourceChannel.getEchoToAlias(), e.getMessage());
        }
      }
      for (String alias : targetAliases) {
        if (alias == null || alias.isBlank()) {
          continue;
        }
        try {
          connectionManager.sendCapturedMediaLinkByEchoToAlias(sourceProtocol, sender,
              replyContext == null ? null : replyContext.senderId(),
              MediaStore.mediaTypeLabel(effectiveContentType), publicUrl, alias.trim());
        } catch (Exception e) {
          log.warn("Unable to send captured image link to {}: {}", alias, e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Media capture failed for sourceAlias={} errorType={}", sourceChannel.getEchoToAlias(), e.getClass().getSimpleName());
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

  private DownloadedMedia download(String imageUrl, int maxFileSizeMb, Map<String, String> requestHeaders,
      boolean configuredService) throws java.io.IOException {
    if (imageUrl == null || imageUrl.isBlank()) {
      throw new IllegalArgumentException("Missing image URL");
    }
    int maxBytes = (int) Math.min(Integer.MAX_VALUE - 1L, Math.max(1, maxFileSizeMb) * 1024L * 1024L);
    URI uri = URI.create(imageUrl);
    var headers = requestHeaders == null ? Map.<String, String>of() : requestHeaders;
    var result = configuredService
        ? PublicHttpFetcher.getConfiguredService(uri, headers, 10000, 30000, maxBytes)
        : PublicHttpFetcher.get(uri, headers, 10000, 30000, maxBytes, 5);
    return new DownloadedMedia(result.body(), result.contentType());
  }

  static Message capturedLinkMessage(BotConnection connection, String channelId, String channelName,
      String sender, String mediaType, String publicUrl, ReplyContext replyContext) {
    boolean irc = connection != null && connection.getType() == BotConnectionType.IRC_CONNECTION;
    String text = "[ " + sender + " :: " + mediaType + " :: " + publicUrl + " ]";
    Message.Builder reply = Message.builder()
        .id(replyContext != null && replyContext.channelId() != null ? replyContext.channelId() : channelId)
        .target(channelName)
        .messageSource(MessageSource.NONE)
        .message(text)
        .suppressLinkPreviews(true);
    if (!irc && replyContext != null) {
      reply.replyToMessageId(replyContext.messageId())
          .messageThreadId(replyContext.threadId())
          .replyToSenderId(replyContext.senderId());
    }
    return reply.build();
  }

  private String publicMediaUrl(MediaStorageSettingsResponse settings, MediaStoreCreated created) {
    if (created.shortCode() != null && !created.shortCode().isBlank()) {
      return publicBaseUrl(settings.publicUrlPrefix()) + "/m/" + created.shortCode();
    }
    return trimTrailingSlash(settings.publicUrlPrefix()) + "/" + created.id() + "?token=" + created.token();
  }

  private String publicBaseUrl(String publicUrlPrefix) {
    String prefix = trimTrailingSlash(publicUrlPrefix);
    if (prefix.endsWith("/media")) {
      return prefix.substring(0, prefix.length() - "/media".length());
    }
    return prefix;
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
