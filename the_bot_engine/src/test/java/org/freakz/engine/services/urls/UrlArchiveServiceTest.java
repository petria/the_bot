package org.freakz.engine.services.urls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.urlarchive.UrlArchiveStore;
import org.freakz.engine.services.media.MediaStorageSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.json.JsonMapper;

class UrlArchiveServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void archivesUrlResolutionWithRequestSourceMetadata() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    MediaStorageSettingsService settingsService = mock(MediaStorageSettingsService.class);
    when(settingsService.getSettings()).thenReturn(new MediaStorageSettingsResponse(
        true,
        tempDir.toString(),
        "https://example.test/media",
        25,
        7,
        true,
        true,
        null));
    UrlArchiveService service = new UrlArchiveService(settingsService, mapper);
    EngineRequest request = EngineRequest.builder()
        .chatProtocol("irc")
        .network("IRCNet")
        .echoToAlias("IRC-TEST")
        .fromSender("petria")
        .build();
    Channel channel = Channel.builder().name("#test").echoToAlias("IRC-TEST").build();

    service.archive(
        new UrlResolution(URI.create("https://example.com"), "Web", "Example", "author", "description", Duration.ofMinutes(1), null, 10L),
        request,
        channel);

    assertThat(new UrlArchiveStore(tempDir, mapper).listActive())
        .hasSize(1)
        .singleElement()
        .satisfies(item -> {
          assertThat(item.url()).isEqualTo("https://example.com");
          assertThat(item.title()).isEqualTo("Example");
          assertThat(item.sourceProtocol()).isEqualTo("irc");
          assertThat(item.sourceNetwork()).isEqualTo("IRCNet");
          assertThat(item.sourceChannelAlias()).isEqualTo("IRC-TEST");
          assertThat(item.sourceChannelName()).isEqualTo("#test");
          assertThat(item.sourceSender()).isEqualTo("petria");
        });
  }

  @Test
  void skipsArchiveWhenMediaStorageIsDisabled() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    MediaStorageSettingsService settingsService = mock(MediaStorageSettingsService.class);
    when(settingsService.getSettings()).thenReturn(new MediaStorageSettingsResponse(
        false,
        tempDir.toString(),
        "https://example.test/media",
        25,
        7,
        true,
        true,
        null));
    UrlArchiveService service = new UrlArchiveService(settingsService, mapper);

    service.archive(
        new UrlResolution(URI.create("https://example.com"), "Web", "Example", null, null, null, null, null),
        EngineRequest.builder().build(),
        null);

    assertThat(new UrlArchiveStore(tempDir, mapper).listActive()).isEmpty();
  }
}
