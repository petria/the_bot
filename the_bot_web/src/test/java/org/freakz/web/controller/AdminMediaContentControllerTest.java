package org.freakz.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;

import org.freakz.common.media.MediaStore;
import org.freakz.common.media.MediaStoreSource;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

class AdminMediaContentControllerTest {

  @TempDir
  Path tempDir;

  @Test
  void returnsActiveMediaContent() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    new MediaStore(tempDir, mapper).create(
        new byte[] {1, 2, 3},
        "image/png",
        "test.png",
        Duration.ofDays(1),
        new MediaStoreSource("discord", "Discord", "DISCORD-TEST", "test", "petria"));

    AdminMediaContentController.MediaContentResponse response = controller(mapper, settings(true)).getMediaContent();

    assertThat(response.enabled()).isTrue();
    assertThat(response.storageDir()).isEqualTo(tempDir.toString());
    assertThat(response.items()).hasSize(1)
        .singleElement()
        .satisfies(item -> {
          assertThat(item.mediaType()).isEqualTo("image");
          assertThat(item.originalFileName()).isEqualTo("test.png");
          assertThat(item.sourceChannelAlias()).isEqualTo("DISCORD-TEST");
          assertThat(item.sourceSender()).isEqualTo("petria");
          assertThat(item.toString()).doesNotContain("tokenHash");
        });
  }

  @Test
  void disabledMediaStorageReturnsEmptyList() {
    AdminMediaContentController.MediaContentResponse response =
        controller(JsonMapper.builder().findAndAddModules().build(), settings(false)).getMediaContent();

    assertThat(response.enabled()).isFalse();
    assertThat(response.items()).isEmpty();
    assertThat(response.detail()).isEqualTo("Media storage is disabled");
  }

  @Test
  void upstreamFailureReturnsUnavailableResponse() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.getMediaStorageSettings()).thenThrow(new IllegalStateException("engine unavailable"));

    AdminMediaContentController.MediaContentResponse response =
        new AdminMediaContentController(engineClient, JsonMapper.builder().findAndAddModules().build()).getMediaContent();

    assertThat(response.enabled()).isFalse();
    assertThat(response.items()).isEmpty();
    assertThat(response.detail()).contains("engine unavailable");
  }

  private AdminMediaContentController controller(JsonMapper mapper, MediaStorageSettingsResponse settings) {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.getMediaStorageSettings()).thenReturn(ResponseEntity.ok(settings));
    return new AdminMediaContentController(engineClient, mapper);
  }

  private MediaStorageSettingsResponse settings(boolean enabled) {
    return new MediaStorageSettingsResponse(
        enabled,
        tempDir.toString(),
        "https://example.test/media",
        25,
        30,
        true,
        true,
        null);
  }
}
