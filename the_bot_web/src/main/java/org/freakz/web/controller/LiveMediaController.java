package org.freakz.web.controller;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.freakz.common.media.MediaStore;
import org.freakz.common.media.MediaStoreListItem;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.urlarchive.UrlArchiveListItem;
import org.freakz.common.urlarchive.UrlArchiveStore;
import org.freakz.web.channels.ChannelAccessService;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/web/live-media")
public class LiveMediaController {

  private final RestEngineClient engineClient;
  private final JsonMapper jsonMapper;
  private final ChannelAccessService accessService;

  public LiveMediaController(
      RestEngineClient engineClient,
      JsonMapper jsonMapper,
      ChannelAccessService accessService) {
    this.engineClient = engineClient;
    this.jsonMapper = jsonMapper;
    this.accessService = accessService;
  }

  @GetMapping
  public LiveMediaResponse getLiveMedia(@AuthenticationPrincipal BotUserPrincipal principal) {
    try {
      ResponseEntity<MediaStorageSettingsResponse> response = engineClient.getMediaStorageSettings();
      MediaStorageSettingsResponse settings = response.getBody();
      if (!response.getStatusCode().is2xxSuccessful() || settings == null) {
        return LiveMediaResponse.unavailable("Could not load media storage settings");
      }
      if (!Boolean.TRUE.equals(settings.enabled())) {
        return LiveMediaResponse.from(settings, List.of(), "Media storage is disabled");
      }
      if (settings.storageDir() == null || settings.storageDir().isBlank()) {
        return LiveMediaResponse.from(settings, List.of(), "Media storage directory is not configured");
      }

      Path storageDir = Path.of(settings.storageDir());
      List<LiveMediaItem> mediaItems = new MediaStore(storageDir, jsonMapper).listActive().stream()
          .filter(item -> canView(principal, item.sourceProtocol(), item.sourceChannelAlias()))
          .map(LiveMediaItem::fromMedia)
          .toList();
      List<LiveMediaItem> urlItems = new UrlArchiveStore(storageDir, jsonMapper).listActive().stream()
          .filter(item -> canView(principal, item.sourceProtocol(), item.sourceChannelAlias()))
          .map(LiveMediaItem::fromUrl)
          .toList();
      List<LiveMediaItem> items = Stream.concat(mediaItems.stream(), urlItems.stream())
          .sorted(Comparator
              .comparing(LiveMediaItem::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
              .thenComparing(LiveMediaItem::type, Comparator.nullsLast(Comparator.naturalOrder()))
              .thenComparing(LiveMediaItem::id, Comparator.nullsLast(Comparator.naturalOrder())))
          .toList();
      return LiveMediaResponse.from(settings, items, settings.detail());
    } catch (Exception e) {
      return LiveMediaResponse.unavailable(e.getMessage());
    }
  }

  private boolean canView(BotUserPrincipal principal, String sourceProtocol, String sourceChannelAlias) {
    return sourceChannelAlias != null
        && !sourceChannelAlias.isBlank()
        && accessService.canView(principal, sourceProtocol, sourceChannelAlias);
  }

  public record LiveMediaResponse(
      boolean enabled,
      String storageDir,
      String publicUrlPrefix,
      String detail,
      List<LiveMediaItem> items) {

    static LiveMediaResponse from(
        MediaStorageSettingsResponse settings,
        List<LiveMediaItem> items,
        String detail) {
      return new LiveMediaResponse(
          Boolean.TRUE.equals(settings.enabled()),
          settings.storageDir(),
          settings.publicUrlPrefix(),
          detail,
          items == null ? List.of() : items);
    }

    static LiveMediaResponse unavailable(String detail) {
      return new LiveMediaResponse(false, null, null, detail, List.of());
    }
  }

  public record LiveMediaItem(
      String type,
      String id,
      String shortCode,
      Instant createdAt,
      Instant expiresAt,
      String sourceProtocol,
      String sourceNetwork,
      String sourceChannelAlias,
      String sourceChannelName,
      String sourceSender,
      String contentType,
      String mediaType,
      String originalFileName,
      Long sizeBytes,
      String url,
      String provider,
      String title,
      String author,
      String description,
      Duration duration,
      Instant publishedAt,
      Long viewCount) {

    static LiveMediaItem fromMedia(MediaStoreListItem item) {
      return new LiveMediaItem(
          "media",
          item.id(),
          item.shortCode(),
          item.createdAt(),
          item.expiresAt(),
          item.sourceProtocol(),
          item.sourceNetwork(),
          item.sourceChannelAlias(),
          item.sourceChannelName(),
          item.sourceSender(),
          item.contentType(),
          item.mediaType(),
          item.originalFileName(),
          item.sizeBytes(),
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
    }

    static LiveMediaItem fromUrl(UrlArchiveListItem item) {
      return new LiveMediaItem(
          "url",
          item.id(),
          item.shortCode(),
          item.createdAt(),
          item.expiresAt(),
          item.sourceProtocol(),
          item.sourceNetwork(),
          item.sourceChannelAlias(),
          item.sourceChannelName(),
          item.sourceSender(),
          null,
          null,
          null,
          null,
          item.url(),
          item.provider(),
          item.title(),
          item.author(),
          item.description(),
          item.duration(),
          item.publishedAt(),
          item.viewCount());
    }
  }
}
