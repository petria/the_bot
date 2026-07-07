package org.freakz.web.controller;

import java.nio.file.Path;
import java.util.List;

import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.urlarchive.UrlArchiveListItem;
import org.freakz.common.urlarchive.UrlArchiveStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/web/admin/collected-urls")
public class AdminCollectedUrlsController {

  private final RestEngineClient engineClient;
  private final JsonMapper jsonMapper;

  public AdminCollectedUrlsController(RestEngineClient engineClient, JsonMapper jsonMapper) {
    this.engineClient = engineClient;
    this.jsonMapper = jsonMapper;
  }

  @GetMapping
  public CollectedUrlsResponse getCollectedUrls() {
    try {
      ResponseEntity<MediaStorageSettingsResponse> response = engineClient.getMediaStorageSettings();
      MediaStorageSettingsResponse settings = response.getBody();
      if (!response.getStatusCode().is2xxSuccessful() || settings == null) {
        return CollectedUrlsResponse.unavailable("Could not load media storage settings");
      }
      if (!Boolean.TRUE.equals(settings.enabled())) {
        return CollectedUrlsResponse.from(settings, List.of(), "Media storage is disabled");
      }
      if (settings.storageDir() == null || settings.storageDir().isBlank()) {
        return CollectedUrlsResponse.from(settings, List.of(), "Media storage directory is not configured");
      }
      List<UrlArchiveListItem> items = new UrlArchiveStore(Path.of(settings.storageDir()), jsonMapper).listActive();
      return CollectedUrlsResponse.from(settings, items, settings.detail());
    } catch (Exception e) {
      return CollectedUrlsResponse.unavailable(e.getMessage());
    }
  }

  public record CollectedUrlsResponse(
      boolean enabled,
      String storageDir,
      String detail,
      List<UrlArchiveListItem> items) {

    static CollectedUrlsResponse from(
        MediaStorageSettingsResponse settings,
        List<UrlArchiveListItem> items,
        String detail) {
      return new CollectedUrlsResponse(
          Boolean.TRUE.equals(settings.enabled()),
          settings.storageDir(),
          detail,
          items == null ? List.of() : items);
    }

    static CollectedUrlsResponse unavailable(String detail) {
      return new CollectedUrlsResponse(false, null, detail, List.of());
    }
  }
}
