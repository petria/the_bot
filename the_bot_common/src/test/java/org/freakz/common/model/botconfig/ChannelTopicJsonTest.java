package org.freakz.common.model.botconfig;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelTopicJsonTest {

  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  void topicSettingsRoundTripThroughJson() throws Exception {
    Channel source = Channel.builder()
        .id("irc-1")
        .name("#test")
        .type("IrcPublic")
        .echoToAlias("IRC-TEST")
        .manageTopic(true)
        .topic("Guarded topic")
        .build();

    Channel result = mapper.readValue(mapper.writeValueAsString(source), Channel.class);

    assertThat(result.getManageTopic()).isTrue();
    assertThat(result.getTopic()).isEqualTo("Guarded topic");
  }

  @Test
  void oldJsonDefaultsTopicSettingsToNull() throws Exception {
    Channel result = mapper.readValue("{\"name\":\"#old\"}", Channel.class);

    assertThat(result.getManageTopic()).isNull();
    assertThat(result.getTopic()).isNull();
  }
}
