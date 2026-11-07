package org.freakz.common.model.botconfig;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelMediaJsonTest {
  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  void capturedUrlSettingRoundTripsAndParticipatesInEquality() throws Exception {
    Channel channel = Channel.builder().id("telegram-1").captureImages(true).showCapturedUrl(true).build();
    Channel restored = mapper.readValue(mapper.writeValueAsString(channel), Channel.class);
    assertThat(restored.getShowCapturedUrl()).isTrue();
    assertThat(restored).isEqualTo(channel);
    assertThat(restored.hashCode()).isEqualTo(channel.hashCode());
    restored.setShowCapturedUrl(false);
    assertThat(restored).isNotEqualTo(channel);
  }

  @Test
  void oldConfigurationsDoNotEnableSourceReplies() throws Exception {
    Channel channel = mapper.readValue("{\"captureImages\":true}", Channel.class);
    assertThat(Boolean.TRUE.equals(channel.getShowCapturedUrl())).isFalse();
  }
}
