package org.freakz.web.controller;

import java.nio.file.Path;
import java.util.List;

import org.freakz.common.media.MediaStore;
import org.freakz.common.media.MediaStoreListItem;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/web/admin/media-content")
public class AdminMediaContentController {

  private final RestEngineClient engineClient;
  private final JsonMapper jsonMapper;

  public AdminMediaContentController(RestEngineClient engineClient, JsonMapper jsonMapper) {
    this.engineClient = engineClient;
    this.jsonMapper = jsonMapper;
  }

  @GetMapping
  public MediaContentResponse getMediaContent() {
    try {
      ResponseEntity<MediaStorageSettingsResponse> response = engineClient.getMediaStorageSettings();
      MediaStorageSettingsResponse settings = response.getBody();
      if (!response.getStatusCode().is2xxSuccessful() || settings == null) {
        return MediaContentResponse.unavailable("Could not load media storage settings");
      }
      if (!Boolean.TRUE.equals(settings.enabled())) {
        return MediaContentResponse.from(settings, List.of(), "Media storage is disabled");
      }
      if (settings.storageDir() == null || settings.storageDir().isBlank()) {
        return MediaContentResponse.from(settings, List.of(), "Media storage directory is not configured");
      }
      List<MediaStoreListItem> items = new MediaStore(Path.of(settings.storageDir()), jsonMapper).listActive();
      return MediaContentResponse.from(settings, items, settings.detail());
    } catch (Exception e) {
      return MediaContentResponse.unavailable(e.getMessage());
    }
  }

  public record MediaContentResponse(
      boolean enabled,
      String storageDir,
      String publicUrlPrefix,
      String detail,
      List<MediaStoreListItem> items) {

    static MediaContentResponse from(
        MediaStorageSettingsResponse settings,
        List<MediaStoreListItem> items,
        String detail) {
      return new MediaContentResponse(
          Boolean.TRUE.equals(settings.enabled()),
          settings.storageDir(),
          settings.publicUrlPrefix(),
          detail,
          items == null ? List.of() : items);
    }

    static MediaContentResponse unavailable(String detail) {
      return new MediaContentResponse(false, null, null, detail, List.of());
    }
  }
}
