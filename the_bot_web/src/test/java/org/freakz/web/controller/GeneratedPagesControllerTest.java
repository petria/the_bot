package org.freakz.web.controller;

import org.freakz.common.config.TheBotProperties;
import org.freakz.common.generated.GeneratedPageCreated;
import org.freakz.common.generated.GeneratedPagePublicResponse;
import org.freakz.common.generated.GeneratedPageStore;
import org.freakz.web.config.TheBotWebProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratedPagesControllerTest {

  @TempDir
  Path tempDir;

  @Test
  void returnsGeneratedPageForValidSecretToken() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    GeneratedPageCreated created =
        new GeneratedPageStore(tempDir, mapper)
            .create("TestPage", "Generated", Map.of("value", 123), Duration.ofDays(7));

    GeneratedPagePublicResponse response =
        controller(mapper).getGeneratedPage(created.id(), created.token());

    assertThat(response.id()).isEqualTo(created.id());
    assertThat(response.componentType()).isEqualTo("TestPage");
    assertThat(response.props()).containsEntry("value", 123);
  }

  @Test
  void rejectsWrongSecretToken() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    GeneratedPageCreated created =
        new GeneratedPageStore(tempDir, mapper)
            .create("TestPage", "Generated", Map.of("value", 123), Duration.ofDays(7));

    assertThatThrownBy(() -> controller(mapper).getGeneratedPage(created.id(), "wrong-token"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
  }

  private GeneratedPagesController controller(JsonMapper mapper) {
    TheBotProperties properties = new TheBotProperties();
    properties.setDataDir(tempDir.toString());
    TheBotWebProperties webProperties = new TheBotWebProperties();
    webProperties.setUsersFile(tempDir.resolve("users.json").toString());
    return new GeneratedPagesController(properties, webProperties, mapper);
  }
}
