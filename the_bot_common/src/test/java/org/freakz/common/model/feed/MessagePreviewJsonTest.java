package org.freakz.common.model.feed;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class MessagePreviewJsonTest {
  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  void capturedLinkKeepsPreviewAndThreadSettingsAcrossSerialization() {
    Message source = Message.builder().id("chat").message("https://bot.test/m/abc12")
        .replyToMessageId("42").messageThreadId("7").suppressLinkPreviews(true).build();
    Message restored = mapper.readValue(mapper.writeValueAsString(source), Message.class);
    assertThat(restored).isEqualTo(source);
    assertThat(restored.isSuppressLinkPreviews()).isTrue();
  }

  @Test
  void existingMessagesKeepDefaultPreviewBehavior() {
    Message restored = mapper.readValue("{\"message\":\"hello\",\"timestamp\":0,\"requestTimestamp\":0}", Message.class);
    assertThat(restored.isSuppressLinkPreviews()).isFalse();
  }
}
