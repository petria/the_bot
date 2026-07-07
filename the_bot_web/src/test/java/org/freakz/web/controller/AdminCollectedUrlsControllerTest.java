package org.freakz.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;

import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.urlarchive.UrlArchiveSource;
import org.freakz.common.urlarchive.UrlArchiveStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;

import tools.jackson.databind.json.JsonMapper;

class AdminCollectedUrlsControllerTest {

  @TempDir
  Path tempDir;

  @Test
  void returnsActiveCollectedUrls() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    new UrlArchiveStore(tempDir, mapper).create(
        "https://example.com",
        "Web",
        "Example",
        "author",
        "description",
        null,
        null,
        null,
        Duration.ofDays(1),
        new UrlArchiveSource("irc", "IRCNet", "IRC-TEST", "#test", "petria"));

    AdminCollectedUrlsController.CollectedUrlsResponse response = controller(mapper, settings(true)).getCollectedUrls();

    assertThat(response.enabled()).isTrue();
    assertThat(response.storageDir()).isEqualTo(tempDir.toString());
    assertThat(response.items()).hasSize(1)
        .singleElement()
        .satisfies(item -> {
          assertThat(item.url()).isEqualTo("https://example.com");
          assertThat(item.provider()).isEqualTo("Web");
          assertThat(item.title()).isEqualTo("Example");
          assertThat(item.sourceProtocol()).isEqualTo("irc");
          assertThat(item.sourceChannelAlias()).isEqualTo("IRC-TEST");
          assertThat(item.sourceSender()).isEqualTo("petria");
        });
  }

  @Test
  void disabledMediaStorageReturnsEmptyList() {
    AdminCollectedUrlsController.CollectedUrlsResponse response =
        controller(JsonMapper.builder().findAndAddModules().build(), settings(false)).getCollectedUrls();

    assertThat(response.enabled()).isFalse();
    assertThat(response.items()).isEmpty();
    assertThat(response.detail()).isEqualTo("Media storage is disabled");
  }

  @Test
  void upstreamFailureReturnsUnavailableResponse() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.getMediaStorageSettings()).thenThrow(new IllegalStateException("engine unavailable"));

    AdminCollectedUrlsController.CollectedUrlsResponse response =
        new AdminCollectedUrlsController(engineClient, JsonMapper.builder().findAndAddModules().build()).getCollectedUrls();

    assertThat(response.enabled()).isFalse();
    assertThat(response.items()).isEmpty();
    assertThat(response.detail()).contains("engine unavailable");
  }

  private AdminCollectedUrlsController controller(JsonMapper mapper, MediaStorageSettingsResponse settings) {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.getMediaStorageSettings()).thenReturn(ResponseEntity.ok(settings));
    return new AdminCollectedUrlsController(engineClient, mapper);
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
