package org.freakz.web.controller;

import org.freakz.common.media.MediaStore;
import org.freakz.common.media.MediaStoreCreated;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaControllerTest {

  @TempDir
  Path tempDir;

  @Test
  void returnsHtmlNotFoundForInvalidShortLink() {
    ResponseEntity<?> response = controller().getShortMedia("missing");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
    assertThat(response.getBody()).isInstanceOf(String.class);
    assertThat((String) response.getBody()).contains(PublicLinkErrorResponse.notFoundTitle());
  }

  @Test
  void returnsHtmlNotFoundForInvalidLongLink() {
    ResponseEntity<?> response = controller().getMedia("missing", "bad-token");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
    assertThat(response.getBody()).isInstanceOf(String.class);
    assertThat((String) response.getBody()).contains(PublicLinkErrorResponse.notFoundTitle());
  }

  @Test
  void returnsMediaForValidShortLink() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    MediaStoreCreated created = new MediaStore(tempDir, mapper).create(
        new byte[] {1, 2, 3},
        "image/png",
        "test.png",
        Duration.ofDays(1),
        null);

    ResponseEntity<?> response = controller(mapper).getShortMedia(created.shortCode());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
    assertThat(response.getBody()).isInstanceOf(FileSystemResource.class);
  }

  private MediaController controller() {
    return controller(JsonMapper.builder().findAndAddModules().build());
  }

  private MediaController controller(JsonMapper mapper) {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.getMediaStorageSettings()).thenReturn(ResponseEntity.ok(new MediaStorageSettingsResponse(
        true,
        tempDir.toString(),
        "https://example.test/m",
        10,
        7,
        true,
        true,
        null)));
    return new MediaController(engineClient, mapper);
  }
}
