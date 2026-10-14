package org.freakz.io.connections;

import org.freakz.common.chat.BotSelfIdentity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chat platforms deliver mentions as part of the message text, so the connection has to hand the
 * engine a command without the mention in front of it.
 */
class BotConnectionTest {

  private static class TestConnection extends BotConnection {
    void useSelfIdentity(BotSelfIdentity selfIdentity) {
      setSelfIdentity(selfIdentity);
    }
  }

  @Test
  void stripsLeadingSelfMentionBeforeCommandOnceIdentityIsKnown() {
    TestConnection connection = new TestConnection();
    connection.useSelfIdentity(new BotSelfIdentity(
        "discord", "Hokan", List.of("1099326189174259782", "Hokan")));

    assertThat(connection.stripLeadingMentionBeforeCommand("<@1099326189174259782> !ping")).isEqualTo("!ping");
    assertThat(connection.stripLeadingMentionBeforeCommand("@Hokan !ping")).isEqualTo("!ping");
    assertThat(connection.isBotMentioned("<@1099326189174259782> !ping")).isTrue();
  }

  @Test
  void keepsMessageUnchangedBeforeSelfIdentityIsResolved() {
    TestConnection connection = new TestConnection();

    assertThat(connection.stripLeadingMentionBeforeCommand("@Hokan !ping")).isEqualTo("@Hokan !ping");
    assertThat(connection.isBotMentioned("@Hokan !ping")).isFalse();
  }

  @Test
  void keepsChatThatIsNotACommandUnchanged() {
    TestConnection connection = new TestConnection();
    connection.useSelfIdentity(new BotSelfIdentity("discord", "Hokan", List.of("Hokan")));

    assertThat(connection.stripLeadingMentionBeforeCommand("@Hokan hello")).isEqualTo("@Hokan hello");
    assertThat(connection.stripLeadingMentionBeforeCommand("@Hokan")).isEqualTo("@Hokan");
    assertThat(connection.stripLeadingMentionBeforeCommand(null)).isNull();
    assertThat(connection.stripLeadingMentionBeforeCommand("   ")).isEqualTo("   ");
  }
}
