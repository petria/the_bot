package org.freakz.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeConfigStoreTest {

  @TempDir
  Path tempDir;

  @Test
  void updatesOnlyMatchingIrcChannelAndKeepsOtherConfig() throws Exception {
    Path config = tempDir.resolve("DEV.the_bot_config.json");
    Files.writeString(config, """
        {
          "botConfig":{"botName":"Hokan"},
          "ircServerConfigs":[{"channelList":[
            {"echoToAlias":"IRC-FIRST","topic":"old"},
            {"echoToAlias":"IRC-SECOND","topic":"keep"}
          ]}]
        }
        """);

    assertThat(RuntimeConfigStore.updateIrcChannelTopic(
        config, "irc-first", "new topic", JsonMapper.builder().build())).isTrue();

    String json = Files.readString(config);
    assertThat(json).contains("\"topic\" : \"new topic\"");
    assertThat(json).contains("\"topic\" : \"keep\"");
    assertThat(json).contains("\"botName\" : \"Hokan\"");
  }

  @Test
  void returnsFalseWhenAliasDoesNotExist() throws Exception {
    Path config = tempDir.resolve("config.json");
    Files.writeString(config, "{\"ircServerConfigs\":[]}");

    assertThat(RuntimeConfigStore.updateIrcChannelTopic(
        config, "missing", "topic", JsonMapper.builder().build())).isFalse();
  }
}
