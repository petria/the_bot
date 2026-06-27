package org.freakz.common.model.connectionmanager;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelUserJsonTest {

  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  void readsChannelUserWithAwayProperty() throws Exception {
    String json = """
        {"channelUsers":[{"account":"petria","nick":"petria","displayPrefix":"@","channelModes":["@"],"channelRoles":["operator"],"away":false}]}
        """;

    ChannelUsersByEchoToAliasResponse response = mapper.readValue(json, ChannelUsersByEchoToAliasResponse.class);

    assertThat(response.getChannelUsers()).hasSize(1);
    assertThat(response.getChannelUsers().getFirst().getAccount()).isEqualTo("petria");
    assertThat(response.getChannelUsers().getFirst().getDisplayPrefix()).isEqualTo("@");
    assertThat(response.getChannelUsers().getFirst().getChannelModes()).containsExactly("@");
    assertThat(response.getChannelUsers().getFirst().getChannelRoles()).containsExactly("operator");
    assertThat(response.getChannelUsers().getFirst().isAway()).isFalse();
  }
}
