package org.freakz.common.model.engine.system;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class HermesFallbackCompatibilityTest {

  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  void allowsMissingResponseEnabled() throws Exception {
    HermesFallbackSettingsResponse response = mapper.readValue("""
        {
          "baseUrl": "http://192.168.0.55:11434/v1",
          "model": "qwen3.6:35b-a3b",
          "profiles": []
        }
        """, HermesFallbackSettingsResponse.class);

    assertThat(response.enabled()).isNull();
  }

  @Test
  void allowsMissingUpdateEnabled() throws Exception {
    HermesFallbackUpdateRequest request = mapper.readValue("""
        {
          "baseUrl": "http://192.168.0.55:11434/v1",
          "model": "qwen3.6:35b-a3b"
        }
        """, HermesFallbackUpdateRequest.class);

    assertThat(request.enabled()).isNull();
  }

  @Test
  void readsBackendConfigPayload() throws Exception {
    HermesBackendConfigResponse response = mapper.readValue("""
        {
          "profiles": [
            {
              "id": "ai-command",
              "label": "Hermes AI command",
              "provider": "ollama",
              "baseUrl": "http://192.168.0.55:11434/v1",
              "model": "qwen3.6:35b-a3b",
              "apiMode": "chat-completions",
              "timeoutSeconds": 120,
              "healthy": true,
              "toolCapable": true,
              "contextWindow": 32768
            }
          ]
        }
        """, HermesBackendConfigResponse.class);

    assertThat(response.profiles()).hasSize(1);
    assertThat(response.profiles().get(0).id()).isEqualTo("ai-command");
    assertThat(response.profiles().get(0).provider()).isEqualTo("ollama");
    assertThat(response.profiles().get(0).contextWindow()).isEqualTo(32768);
  }
}
