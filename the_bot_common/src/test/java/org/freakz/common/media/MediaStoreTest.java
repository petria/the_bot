package org.freakz.common.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

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
          assertThat(result.record().getSourceProtocol()).isEqualTo("discord");
          assertThat(result.file()).exists();
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
}
