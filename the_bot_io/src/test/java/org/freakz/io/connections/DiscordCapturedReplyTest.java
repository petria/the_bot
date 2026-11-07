package org.freakz.io.connections;

import org.freakz.common.model.feed.Message;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerThreadChannel;
import org.javacord.api.entity.message.Messageable;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscordCapturedReplyTest {
  @Test
  void sendsCapturedReplyInsideOriginalDiscordThread() throws Exception {
    DiscordApi api = mock(DiscordApi.class);
    ServerThreadChannel thread = mock(ServerThreadChannel.class);
    when(thread.getId()).thenReturn(123L);
    when(thread.asServerThreadChannel()).thenReturn(Optional.of(thread));
    when(api.getChannels()).thenReturn(Set.of());
    when(api.getServerThreadChannels()).thenReturn(Set.of(thread));
    var connection = new DiscordServerConnection(null) {
      @Override void sendDiscordMessage(Messageable target, String text, Message message) {
        assertThat(target).isSameAs(thread);
        assertThat(text).isEqualTo("<https://bot.test/m/abc12>");
        assertThat(message.getReplyToMessageId()).isEqualTo("42");
        sent = true;
      }
      boolean sent;
    };
    var field = DiscordServerConnection.class.getDeclaredField("api");
    field.setAccessible(true);
    field.set(connection, api);
    connection.sendMessageTo(Message.builder().id("123").message("<https://bot.test/m/abc12>")
        .replyToMessageId("42").suppressLinkPreviews(true).build());
    assertThat(connection.sent).isTrue();
  }
}
