package org.freakz.common.generated;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedPageStoreTest {

  @TempDir
  Path tempDir;

  @Test
  void readsPageWithValidTokenOnly() throws Exception {
    GeneratedPageStore store = new GeneratedPageStore(tempDir, mapper());

    GeneratedPageCreated created =
        store.create("TestPage", "Title", Map.of("value", 123), Duration.ofDays(7));

    assertThat(store.readPublic(created.id(), created.token())).isPresent();
    assertThat(store.readPublic(created.id(), "wrong-token")).isEmpty();
  }

  @Test
  void rejectsExpiredPage() throws Exception {
    Instant now = Instant.parse("2026-05-16T12:00:00Z");
    GeneratedPageStore writer =
        new GeneratedPageStore(tempDir, mapper(), Clock.fixed(now, ZoneOffset.UTC));
    GeneratedPageCreated created =
        writer.create("TestPage", "Title", Map.of("value", 123), Duration.ofSeconds(1));

    GeneratedPageStore reader =
        new GeneratedPageStore(tempDir, mapper(), Clock.fixed(now.plusSeconds(2), ZoneOffset.UTC));

    assertThat(reader.readPublic(created.id(), created.token())).isEmpty();
  }

  private JsonMapper mapper() {
    return JsonMapper.builder().findAndAddModules().build();
  }
}
