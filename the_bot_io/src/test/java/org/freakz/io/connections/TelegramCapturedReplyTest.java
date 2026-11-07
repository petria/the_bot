package org.freakz.io.connections;

import org.freakz.common.model.feed.Message;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramCapturedReplyTest {
  @Test
  void capturedLinkRepliesToOriginalMessageInSameTopicWithoutPreview() {
    Message message = Message.builder().id("-123").message("https://bot.test/m/abc12")
        .replyToMessageId("42").messageThreadId("7").suppressLinkPreviews(true).build();
    var outgoing = new TelegramConnection(null).createTextMessage(message);
    assertThat(outgoing.getChatId()).isEqualTo("-123");
    assertThat(outgoing.getText()).isEqualTo("https://bot.test/m/abc12");
    assertThat(outgoing.getReplyToMessageId()).isEqualTo(42);
    assertThat(outgoing.getMessageThreadId()).isEqualTo(7);
    assertThat(outgoing.getDisableWebPagePreview()).isTrue();
  }

  @Test
  void normalMessagesKeepPreviewsEnabled() {
    var outgoing = new TelegramConnection(null).createTextMessage(
        Message.builder().id("-123").message("hello").build());
    assertThat(outgoing.getDisableWebPagePreview()).isFalse();
    assertThat(outgoing.getReplyToMessageId()).isNull();
  }
}
