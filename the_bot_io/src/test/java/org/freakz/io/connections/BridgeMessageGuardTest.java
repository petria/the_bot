package org.freakz.io.connections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BridgeMessageGuardTest {

  @Test
  void detectsBridgeFormattedMessages() {
    assertThat(BridgeMessageGuard.shouldSkipEcho("<unh@IRC>: hello")).isTrue();
    assertThat(BridgeMessageGuard.shouldSkipEcho("<Hokan@Discord>: hello")).isTrue();
    assertThat(BridgeMessageGuard.shouldSkipEcho("\u0002\u0002<Petria@Telegram>: hello")).isTrue();
    assertThat(BridgeMessageGuard.shouldSkipEcho("\u0002\u0002\u0002<Petria@Telegram>: hello")).isTrue();
    assertThat(BridgeMessageGuard.shouldSkipEcho("<Petria@WhatsApp>: hello")).isTrue();
  }

  @Test
  void doesNotDetectNormalMessages() {
    assertThat(BridgeMessageGuard.shouldSkipEcho("hello")).isFalse();
    assertThat(BridgeMessageGuard.shouldSkipEcho("!ping")).isFalse();
    assertThat(BridgeMessageGuard.shouldSkipEcho("<Hokan@Dicord>: hello")).isFalse();
    assertThat(BridgeMessageGuard.shouldSkipEcho(null)).isFalse();
  }
}
