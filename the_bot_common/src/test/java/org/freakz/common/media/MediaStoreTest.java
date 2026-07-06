package org.freakz.common.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class MediaStoreTest {

  @TempDir
  java.nio.file.Path tempDir;

  @Test
  void storesAndReadsMediaWithToken() throws Exception {
    MediaStore store = new MediaStore(tempDir, new JsonMapper());

    MediaStoreCreated created = store.create(
        new byte[] {1, 2, 3},
        "image/png",
        "test.png",
        Duration.ofDays(1),
        new MediaStoreSource("discord", "Discord", "DISCORD-TEST", "test", "petria"));

    assertThat(store.readPublic(created.id(), created.token())).isPresent()
        .get()
        .satisfies(result -> {
          assertThat(result.record().getContentType()).isEqualTo("image/png");
          assertThat(result.record().getShortCode()).isEqualTo(created.shortCode());
          assertThat(result.record().getSourceProtocol()).isEqualTo("discord");
          assertThat(result.file()).exists();
        });
    assertThat(created.shortCode()).matches("[a-zA-Z0-9]{5}");
  }

  @Test
  void readsMediaWithShortCode() throws Exception {
    MediaStore store = new MediaStore(tempDir, new JsonMapper());

    MediaStoreCreated created = store.create(
        new byte[] {1, 2, 3},
        "image/png",
        "test.png",
        Duration.ofDays(1),
        null);

    assertThat(store.readPublicByShortCode(created.shortCode())).isPresent()
        .get()
        .satisfies(result -> {
          assertThat(result.record().getId()).isEqualTo(created.id());
          assertThat(result.file()).exists();
        });
  }

  @Test
  void storesAndReadsVideoMedia() throws Exception {
    MediaStore store = new MediaStore(tempDir, new JsonMapper());

    MediaStoreCreated created = store.create(
        new byte[] {1, 2, 3},
        "video/mp4",
        "test.mp4",
        Duration.ofDays(1),
        null);

    assertThat(store.readPublicByShortCode(created.shortCode())).isPresent()
        .get()
        .satisfies(result -> {
          assertThat(result.record().getContentType()).isEqualTo("video/mp4");
          assertThat(result.file().getFileName().toString()).endsWith(".mp4");
        });
  }

  @Test
  void storesAndReadsAudioMedia() throws Exception {
    MediaStore store = new MediaStore(tempDir, new JsonMapper());

    MediaStoreCreated created = store.create(
        new byte[] {1, 2, 3},
        "audio/ogg; codecs=opus",
        "test.ogg",
        Duration.ofDays(1),
        null);

    assertThat(store.readPublicByShortCode(created.shortCode())).isPresent()
        .get()
        .satisfies(result -> {
          assertThat(result.record().getContentType()).isEqualTo("audio/ogg");
          assertThat(result.file().getFileName().toString()).endsWith(".ogg");
        });
  }

  @Test
  void rejectsWrongToken() throws Exception {
    MediaStore store = new MediaStore(tempDir, new JsonMapper());
    MediaStoreCreated created = store.create(
        new byte[] {1, 2, 3},
        "image/png",
        "test.png",
        Duration.ofDays(1),
        null);

    assertThat(store.readPublic(created.id(), "wrong")).isEmpty();
  }

  @Test
  void rejectsUnsupportedMediaType() {
    MediaStore store = new MediaStore(tempDir, new JsonMapper());

    assertThatThrownBy(() -> store.create(
        new byte[] {1, 2, 3},
        "application/pdf",
        "test.pdf",
        Duration.ofDays(1),
        null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported");
  }

  @Test
  void listsActiveMediaNewestFirstWithoutTokenHash() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    Instant base = Instant.parse("2026-01-01T12:00:00Z");
    MediaStore olderStore = new MediaStore(tempDir, mapper, Clock.fixed(base, ZoneOffset.UTC));
    MediaStore newerStore = new MediaStore(tempDir, mapper, Clock.fixed(base.plusSeconds(60), ZoneOffset.UTC));

    MediaStoreCreated older = olderStore.create(
        new byte[] {1, 2, 3},
        "image/png",
        "older.png",
        Duration.ofDays(1),
        new MediaStoreSource("discord", "Discord", "DISCORD-TEST", "test", "petria"));
    MediaStoreCreated newer = newerStore.create(
        new byte[] {4, 5, 6},
        "audio/ogg",
        "newer.ogg",
        Duration.ofDays(1),
        new MediaStoreSource("telegram", "Telegram", "TELEGRAM-TEST", "test", "petri"));

    assertThat(newerStore.listActive())
        .extracting(MediaStoreListItem::id)
        .containsExactly(newer.id(), older.id());
    assertThat(newerStore.listActive().getFirst())
        .satisfies(item -> {
          assertThat(item.mediaType()).isEqualTo("audio");
          assertThat(item.sourceProtocol()).isEqualTo("telegram");
          assertThat(item.sourceSender()).isEqualTo("petri");
          assertThat(item.toString()).doesNotContain("tokenHash");
        });
  }

  @Test
  void listActiveCleansExpiredMedia() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    Instant base = Instant.parse("2026-01-01T12:00:00Z");
    MediaStore creator = new MediaStore(tempDir, mapper, Clock.fixed(base, ZoneOffset.UTC));
    MediaStoreCreated created = creator.create(
        new byte[] {1, 2, 3},
        "image/png",
        "expired.png",
        Duration.ofMinutes(1),
        null);

    MediaStore reader = new MediaStore(tempDir, mapper, Clock.fixed(base.plus(Duration.ofMinutes(2)), ZoneOffset.UTC));

    assertThat(reader.listActive()).isEmpty();
    assertThat(tempDir.resolve("metadata").resolve(created.id() + ".json")).doesNotExist();
  }

  @Test
  void listActiveSkipsMetadataWhenFileIsMissing() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    MediaStore store = new MediaStore(tempDir, mapper);
    MediaStoreCreated created = store.create(
        new byte[] {1, 2, 3},
        "image/png",
        "missing.png",
        Duration.ofDays(1),
        null);
    Files.deleteIfExists(tempDir.resolve("files").resolve(created.id() + ".png"));

    assertThat(store.listActive()).isEmpty();
  }
}
