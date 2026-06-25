package org.freakz.engine.services.howto;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HowtoIndexServiceTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void loadsIndexAndFindsBestMatchingChunks() {
    HowtoIndexService service = new HowtoIndexService(jsonMapper, resource("""
        {
          "schemaVersion": 1,
          "generatedAt": "2026-06-25T10:00:00Z",
          "generator": "test",
          "sourceFilter": "git-tracked-files",
          "chunkCount": 2,
          "allowlist": ["the_bot_web/frontend/src/pages"],
          "chunks": [
            {
              "id": "config",
              "title": "Web UI page: Admin Connection Config Page",
              "area": "web-ui",
              "sourcePath": "the_bot_web/frontend/src/pages/AdminConnectionConfigPage.tsx",
              "chunkIndex": 0,
              "keywords": ["Manage Connections", "Save and apply", "IRC"],
              "text": "The Manage Connections page edits IRC, Discord, Telegram, and WhatsApp runtime config."
            },
            {
              "id": "profile",
              "title": "Web UI page: Profile Page",
              "area": "web-ui",
              "sourcePath": "the_bot_web/frontend/src/pages/ProfilePage.tsx",
              "chunkIndex": 0,
              "keywords": ["password"],
              "text": "The profile page lets a logged in user change password."
            }
          ]
        }
        """));

    service.loadIndex();

    assertThat(service.summary().loaded()).isTrue();
    assertThat(service.summary().loadedChunkCount()).isEqualTo(2);
    assertThat(service.search("save irc connection config", 1))
        .hasSize(1)
        .first()
        .satisfies(result -> {
          assertThat(result.score()).isPositive();
          assertThat(result.chunk().id()).isEqualTo("config");
        });
  }

  @Test
  void formatsChatAnswerFromSearchResults() {
    HowtoIndexService service = new HowtoIndexService(jsonMapper, resource("""
        {
          "schemaVersion": 1,
          "generatedAt": "2026-06-25T10:00:00Z",
          "generator": "test",
          "sourceFilter": "git-tracked-files",
          "chunkCount": 1,
          "allowlist": [],
          "chunks": [
            {
              "id": "users",
              "title": "Web UI page: Admin Users Page",
              "area": "web-ui",
              "sourcePath": "the_bot_web/frontend/src/pages/AdminUsersPage.tsx",
              "chunkIndex": 0,
              "keywords": ["Manage Users", "permissions"],
              "text": "The Manage Users page lets admins edit users and permissions."
            }
          ]
        }
        """));

    service.loadIndex();

    assertThat(service.formatChatAnswer("edit user permissions", 3))
        .contains("Howto matches:")
        .contains("Admin Users Page")
        .contains("permissions");
  }

  @Test
  void reportsMissingIndex() {
    Resource missing = new ByteArrayResource(new byte[0]) {
      @Override
      public boolean exists() {
        return false;
      }
    };
    HowtoIndexService service = new HowtoIndexService(jsonMapper, missing);

    service.loadIndex();

    assertThat(service.summary().loaded()).isFalse();
    assertThat(service.formatChatAnswer("anything", 3))
        .isEqualTo("Howto index is not loaded in this bot-engine build.");
  }

  private Resource resource(String json) {
    return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
  }
}
