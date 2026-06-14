package org.freakz.cli.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageSenderTest {

  @Test
  void resolvesBareHostToHttpsThenLocalBotWebPort() {
    assertThat(MessageSender.resolveCandidates("debbox.local"))
        .containsExactly("https://debbox.local", "http://debbox.local:8091");
  }

  @Test
  void preservesExplicitScheme() {
    assertThat(MessageSender.resolveCandidates("https://debbox.local:8091"))
        .containsExactly("https://debbox.local:8091");
  }

  @Test
  void preservesExplicitPortWithoutAdding8091() {
    assertThat(MessageSender.resolveCandidates("debbox.local:8091"))
        .containsExactly("https://debbox.local:8091", "http://debbox.local:8091");
  }
}
