package org.freakz.common.urlarchive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.json.JsonMapper;

class UrlArchiveStoreTest {

  @TempDir
  java.nio.file.Path tempDir;

  @Test
  void storesAndListsUrlMetadataNewestFirst() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    Instant base = Instant.parse("2026-01-01T12:00:00Z");
    UrlArchiveStore olderStore = new UrlArchiveStore(tempDir, mapper, Clock.fixed(base, ZoneOffset.UTC));
    UrlArchiveStore newerStore = new UrlArchiveStore(tempDir, mapper, Clock.fixed(base.plusSeconds(60), ZoneOffset.UTC));

    UrlArchiveRecord older = olderStore.create(
        "https://example.com/older",
        "Web",
        "Older",
        null,
        "Older description",
        null,
        null,
        null,
        Duration.ofDays(1),
        new UrlArchiveSource("irc", "IRCNet", "IRC-TEST", "#test", "petria"));
    UrlArchiveRecord newer = newerStore.create(
        "https://example.com/newer",
        "Web",
        "Newer",
        "author",
        null,
        Duration.ofMinutes(2),
        base,
        12L,
        Duration.ofDays(1),
        new UrlArchiveSource("discord", "Discord", "DISCORD-TEST", "test", "petri"));

    assertThat(newerStore.listActive())
        .extracting(UrlArchiveListItem::id)
        .containsExactly(newer.getId(), older.getId());
    assertThat(newerStore.listActive().getFirst())
        .satisfies(item -> {
          assertThat(item.shortCode()).matches("[a-zA-Z0-9]{6}");
          assertThat(item.url()).isEqualTo("https://example.com/newer");
          assertThat(item.provider()).isEqualTo("Web");
          assertThat(item.duration()).isEqualTo(Duration.ofMinutes(2));
          assertThat(item.publishedAt()).isEqualTo(base);
          assertThat(item.viewCount()).isEqualTo(12L);
          assertThat(item.sourceProtocol()).isEqualTo("discord");
        });
  }

  @Test
  void rejectsMissingUrlOrTitle() {
    UrlArchiveStore store = new UrlArchiveStore(tempDir, new JsonMapper());

    assertThatThrownBy(() -> store.create(
        "",
        "Web",
        "Title",
        null,
        null,
        null,
        null,
        null,
        Duration.ofDays(1),
        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("URL");

    assertThatThrownBy(() -> store.create(
        "https://example.com",
        "Web",
        "",
        null,
        null,
        null,
        null,
        null,
        Duration.ofDays(1),
        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("title");
  }

  @Test
  void listActiveCleansExpiredUrlMetadata() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    Instant base = Instant.parse("2026-01-01T12:00:00Z");
    UrlArchiveStore creator = new UrlArchiveStore(tempDir, mapper, Clock.fixed(base, ZoneOffset.UTC));
    UrlArchiveRecord created = creator.create(
        "https://example.com",
        "Web",
        "Example",
        null,
        null,
        null,
        null,
        null,
        Duration.ofMinutes(1),
        null);

    UrlArchiveStore reader = new UrlArchiveStore(tempDir, mapper, Clock.fixed(base.plus(Duration.ofMinutes(2)), ZoneOffset.UTC));

    assertThat(reader.listActive()).isEmpty();
    assertThat(tempDir.resolve("url-metadata").resolve(created.getId() + ".json")).doesNotExist();
  }
}
