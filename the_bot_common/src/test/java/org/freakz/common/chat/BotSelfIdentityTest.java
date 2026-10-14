package org.freakz.common.chat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BotSelfIdentityTest {

  @Test
  void matchesWhatsAppPnAndLidMentionForms() {
    BotSelfIdentity identity = new BotSelfIdentity(
        "whatsapp",
        "Hokan-DEV",
        List.of("358449125874:2@s.whatsapp.net", "66134711775265:2@lid"));

    assertThat(identity.matches("@66134711775265 so what is next?"))
        .isTrue();
    assertThat(identity.matches("reply to @358449125874 please"))
        .isTrue();
  }

  @Test
  void doesNotMatchAUserWithSimilarPrefix() {
    BotSelfIdentity identity = new BotSelfIdentity("telegram", "Hokan", List.of("12345"));

    assertThat(identity.matches("@123456 hello")).isFalse();
  }

  private static BotSelfIdentity discordIdentity() {
    return new BotSelfIdentity(
        "discord",
        "Hokan",
        Arrays.asList("1099326189174259782", "Hokan", "1099326189174259782", null));
  }

  @Test
  void stripsDiscordUserIdMentionBeforeCommand() {
    BotSelfIdentity identity = discordIdentity();

    assertThat(identity.stripLeadingMentionBeforeCommand("<@1099326189174259782> !ping"))
        .isEqualTo("!ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("<@!1099326189174259782> !ping"))
        .isEqualTo("!ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("<@1099326189174259782> !tucka day"))
        .isEqualTo("!tucka day");
  }

  @Test
  void stripsNamedMentionBeforeCommandIncludingTrailingPunctuation() {
    BotSelfIdentity identity = discordIdentity();

    assertThat(identity.stripLeadingMentionBeforeCommand("@Hokan !ping")).isEqualTo("!ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("@hokan !ping")).isEqualTo("!ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("@Hokan: !ping")).isEqualTo("!ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("@Hokan, !ping")).isEqualTo("!ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("  @Hokan   !ping  ")).isEqualTo("!ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("@Hokan\n!ping")).isEqualTo("!ping");
  }

  @Test
  void stripsRunOfMentionsWhenOneOfThemIsTheBot() {
    BotSelfIdentity identity = discordIdentity();

    assertThat(identity.stripLeadingMentionBeforeCommand("@Hokan @friend !ping")).isEqualTo("!ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("@friend @Hokan !ping")).isEqualTo("!ping");
  }

  @Test
  void keepsPlatformMentionFormsOfOtherPlatformsBeforeCommand() {
    BotSelfIdentity telegramIdentity = new BotSelfIdentity("telegram", "hokanbot", List.of("12345"));
    BotSelfIdentity whatsappIdentity = new BotSelfIdentity(
        "whatsapp",
        "Hokan-DEV",
        List.of("358449125874:2@s.whatsapp.net", "66134711775265:2@lid"));

    assertThat(telegramIdentity.stripLeadingMentionBeforeCommand("@hokanbot !ping")).isEqualTo("!ping");
    assertThat(whatsappIdentity.stripLeadingMentionBeforeCommand("@358449125874 !ping")).isEqualTo("!ping");
    assertThat(whatsappIdentity.stripLeadingMentionBeforeCommand("@Hokan-DEV !ping")).isEqualTo("!ping");
  }

  @Test
  void keepsMessagesWithoutACommandUnchanged() {
    BotSelfIdentity identity = discordIdentity();

    assertThat(identity.stripLeadingMentionBeforeCommand("@Hokan hello")).isEqualTo("@Hokan hello");
    assertThat(identity.stripLeadingMentionBeforeCommand("@Hokan")).isEqualTo("@Hokan");
    assertThat(identity.stripLeadingMentionBeforeCommand("<@1099326189174259782>"))
        .isEqualTo("<@1099326189174259782>");
    assertThat(identity.stripLeadingMentionBeforeCommand("@Hokan are you thinking?"))
        .isEqualTo("@Hokan are you thinking?");
  }

  @Test
  void keepsCommandsAndMentionsOfOthersUnchanged() {
    BotSelfIdentity identity = discordIdentity();

    assertThat(identity.stripLeadingMentionBeforeCommand("!ping")).isEqualTo("!ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("hey @Hokan !ping")).isEqualTo("hey @Hokan !ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("@someone !ping")).isEqualTo("@someone !ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("@10993261891742597820 !ping"))
        .isEqualTo("@10993261891742597820 !ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("@here !ping")).isEqualTo("@here !ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("@everyone !ping")).isEqualTo("@everyone !ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("Hokan !ping")).isEqualTo("Hokan !ping");
    assertThat(identity.stripLeadingMentionBeforeCommand("https://example.com !ping"))
        .isEqualTo("https://example.com !ping");
  }

  @Test
  void keepsNullOrBlankMessageAndIdentityWithoutFormsUnchanged() {
    BotSelfIdentity identity = discordIdentity();

    assertThat(identity.stripLeadingMentionBeforeCommand(null)).isNull();
    assertThat(identity.stripLeadingMentionBeforeCommand("")).isEqualTo("");
    assertThat(identity.stripLeadingMentionBeforeCommand("   ")).isEqualTo("   ");
    assertThat(new BotSelfIdentity("irc", null, List.of()).stripLeadingMentionBeforeCommand("@Hokan !ping"))
        .isEqualTo("@Hokan !ping");
  }
}
