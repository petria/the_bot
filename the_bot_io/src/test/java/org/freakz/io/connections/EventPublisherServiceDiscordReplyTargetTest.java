package org.freakz.io.connections;

import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The reply target written into the engine request is what the Discord connection later resolves,
 * and a direct message channel has no name to resolve.
 */
class EventPublisherServiceDiscordReplyTargetTest {

  @Test
  void answersServerMessagesInTheNamedChannel() {
    ServerChannel serverChannel = mock(ServerChannel.class);
    when(serverChannel.getName()).thenReturn("general");
    MessageCreateEvent event = event(Optional.of(mock(Server.class)), Optional.of(serverChannel), "1033431599708123278");

    assertThat(EventPublisherService.discordReplyTarget(event, "DISCORD-GENERAL", "265828694445129728"))
        .isEqualTo("general");
  }

  @Test
  void fallsBackToChannelIdWhenServerChannelHasNoName() {
    MessageCreateEvent event = event(Optional.of(mock(Server.class)), Optional.empty(), "1033431599708123278");

    assertThat(EventPublisherService.discordReplyTarget(event, "DISCORD-GENERAL", "265828694445129728"))
        .isEqualTo("1033431599708123278");
  }

  @Test
  void answersDirectMessagesThroughTheirPrivateAlias() {
    MessageCreateEvent event = event(Optional.empty(), Optional.empty(), "288971722714251264");

    assertThat(EventPublisherService.discordReplyTarget(event, "PRIVATE-DISCORD-265828694445129728", "265828694445129728"))
        .isEqualTo("PRIVATE-DISCORD-265828694445129728");
  }

  @Test
  void buildsPrivateAliasWhenNoAliasWasResolved() {
    MessageCreateEvent event = event(Optional.empty(), Optional.empty(), "288971722714251264");

    assertThat(EventPublisherService.discordReplyTarget(event, null, "265828694445129728"))
        .isEqualTo("PRIVATE-DISCORD-265828694445129728");
    assertThat(EventPublisherService.discordReplyTarget(event, "  ", "265828694445129728"))
        .isEqualTo("PRIVATE-DISCORD-265828694445129728");
  }

  private MessageCreateEvent event(
      Optional<Server> server, Optional<ServerChannel> serverChannel, String channelId) {
    TextChannel channel = mock(TextChannel.class);
    when(channel.asServerChannel()).thenReturn(serverChannel);
    when(channel.getIdAsString()).thenReturn(channelId);
    MessageCreateEvent event = mock(MessageCreateEvent.class);
    when(event.getServer()).thenReturn(server);
    when(event.getChannel()).thenReturn(channel);
    return event;
  }
}
