package org.freakz.common.model.engine.system;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class HermesFallbackCompatibilityTest {

  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  void defaultsMissingResponseEnabledToFalse() throws Exception {
    HermesFallbackSettingsResponse response = mapper.readValue("""
        {
          "baseUrl": "http://192.168.0.55:11434/v1",
          "model": "qwen3.6:35b-a3b",
          "profiles": []
        }
        """, HermesFallbackSettingsResponse.class);

    assertThat(response.enabled()).isFalse();
  }

  @Test
  void defaultsMissingUpdateEnabledToFalse() throws Exception {
    HermesFallbackUpdateRequest request = mapper.readValue("""
        {
          "baseUrl": "http://192.168.0.55:11434/v1",
          "model": "qwen3.6:35b-a3b"
        }
        """, HermesFallbackUpdateRequest.class);

    assertThat(request.enabled()).isFalse();
  }
}
