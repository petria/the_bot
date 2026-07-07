package org.freakz.engine.services.urls;

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
    if (resolution == null || resolution.url() == null || resolution.title() == null || resolution.title().isBlank()) {
      return;
    }
    try {
      MediaStorageSettingsResponse settings = mediaStorageSettingsService.getSettings();
      if (settings == null || !Boolean.TRUE.equals(settings.enabled()) || settings.storageDir() == null || settings.storageDir().isBlank()) {
        return;
      }
      int retentionDays = settings.retentionDays() == null ? 30 : settings.retentionDays();
      new UrlArchiveStore(Path.of(settings.storageDir()), jsonMapper).create(
          resolution.url().toString(),
          resolution.provider(),
          resolution.title(),
          resolution.author(),
          resolution.description(),
          resolution.duration(),
          resolution.publishedAt(),
          resolution.viewCount(),
          Duration.ofDays(retentionDays),
          new UrlArchiveSource(
              ChatIdentityUtil.sanitize(request.getChatProtocol(), ChatIdentityUtil.resolveProtocol(request.getNetwork())),
              request.getNetwork(),
              request.getEchoToAlias(),
              channel == null ? null : channel.getName(),
              request.getFromSender()));
    } catch (Exception e) {
      log.warn("Unable to archive resolved URL {}: {}", resolution.url(), e.getMessage());
      log.debug("URL archive failure", e);
    }
  }
}
