package org.freakz.engine.services.urls;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.urlarchive.UrlArchiveSource;
import org.freakz.common.urlarchive.UrlArchiveStore;
import org.freakz.engine.services.media.MediaStorageSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

@Service
public class UrlArchiveService {

  private static final Logger log = LoggerFactory.getLogger(UrlArchiveService.class);

  private final MediaStorageSettingsService mediaStorageSettingsService;
  private final JsonMapper jsonMapper;

  public UrlArchiveService(MediaStorageSettingsService mediaStorageSettingsService, JsonMapper jsonMapper) {
    this.mediaStorageSettingsService = mediaStorageSettingsService;
    this.jsonMapper = jsonMapper;
  }

  public void archive(UrlResolution resolution, EngineRequest request, Channel channel) {
    if (resolution == null) {
      return;
    }
    archive(resolution.url(), resolution, request, channel);
  }

  public void archive(URI url, UrlResolution resolution, EngineRequest request, Channel channel) {
    if (url == null) {
      return;
    }
    try {
      MediaStorageSettingsResponse settings = mediaStorageSettingsService.getSettings();
      if (settings == null || !Boolean.TRUE.equals(settings.enabled()) || settings.storageDir() == null || settings.storageDir().isBlank()) {
        return;
      }
      int retentionDays = settings.retentionDays() == null ? 30 : settings.retentionDays();
      new UrlArchiveStore(Path.of(settings.storageDir()), jsonMapper).create(
          url.toString(),
          resolution == null ? null : resolution.provider(),
          resolution == null ? null : resolution.title(),
          resolution == null ? null : resolution.author(),
          resolution == null ? null : resolution.description(),
          resolution == null ? null : resolution.duration(),
          resolution == null ? null : resolution.publishedAt(),
          resolution == null ? null : resolution.viewCount(),
          Duration.ofDays(retentionDays),
          new UrlArchiveSource(
              ChatIdentityUtil.sanitize(request.getChatProtocol(), ChatIdentityUtil.resolveProtocol(request.getNetwork())),
              request.getNetwork(),
              request.getEchoToAlias(),
              channel == null ? null : channel.getName(),
              request.getFromSender()));
    } catch (Exception e) {
      log.warn("Unable to archive URL {}: {}", url, e.getMessage());
      log.debug("URL archive failure", e);
    }
  }
}
