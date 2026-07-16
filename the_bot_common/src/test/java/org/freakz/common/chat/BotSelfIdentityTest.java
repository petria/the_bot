package org.freakz.common.chat;

import org.junit.jupiter.api.Test;

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
}
