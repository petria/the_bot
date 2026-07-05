package org.freakz.web.controller;

import java.nio.file.Path;

import org.freakz.common.media.MediaStore;
import org.freakz.common.media.MediaStoreReadResult;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@RestController
public class MediaController {

  private final RestEngineClient engineClient;
  private final JsonMapper jsonMapper;

  public MediaController(RestEngineClient engineClient, JsonMapper jsonMapper) {
    this.engineClient = engineClient;
    this.jsonMapper = jsonMapper;
  }

  @GetMapping("/media/{id}")
  public ResponseEntity<FileSystemResource> getMedia(
      @PathVariable String id,
      @RequestParam String token) {
    return mediaResponse(settings -> new MediaStore(Path.of(settings.storageDir()), jsonMapper).readPublic(id, token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  @GetMapping("/m/{code}")
  public ResponseEntity<FileSystemResource> getShortMedia(@PathVariable String code) {
    return mediaResponse(settings -> new MediaStore(Path.of(settings.storageDir()), jsonMapper).readPublicByShortCode(code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  private ResponseEntity<FileSystemResource> mediaResponse(MediaReader reader) {
    try {
      MediaStorageSettingsResponse settings = engineClient.getMediaStorageSettings().getBody();
      if (settings == null || !Boolean.TRUE.equals(settings.enabled())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
      MediaStoreReadResult result = reader.read(settings);
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(result.record().getContentType()))
          .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + result.record().getOriginalFileName() + "\"")
          .cacheControl(CacheControl.noStore())
          .body(new FileSystemResource(result.file()));
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
  }

  @FunctionalInterface
  private interface MediaReader {
    MediaStoreReadResult read(MediaStorageSettingsResponse settings) throws Exception;
  }
}
