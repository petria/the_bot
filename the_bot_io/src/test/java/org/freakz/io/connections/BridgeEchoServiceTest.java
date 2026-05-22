package org.freakz.io.connections;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.feed.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BridgeEchoServiceTest {

  @Test
  void echoesNormalMessageToConfiguredTargets() {
    ConnectionManager connectionManager = new ConnectionManager();
    RecordingConnection targetConnection = addTarget(connectionManager, "TARGET", "#target");

    BridgeEchoService.echoToConfiguredTargets(
        connectionManager,
        sourceChannel("SOURCE", "TARGET"),
        "IRC",
        "petria",
        "hello",
        "Hokan");

    assertThat(targetConnection.sentMessages)
        .extracting(Message::getMessage)
        .containsExactly("<petria@IRC>: hello");
  }

  @Test
  void skipsCommandMessages() {
    ConnectionManager connectionManager = new ConnectionManager();
    RecordingConnection targetConnection = addTarget(connectionManager, "TARGET", "#target");

    BridgeEchoService.echoToConfiguredTargets(connectionManager, sourceChannel("SOURCE", "TARGET"), "IRC", "petria", "!ping", "Hokan");
    BridgeEchoService.echoToConfiguredTargets(connectionManager, sourceChannel("SOURCE", "TARGET"), "IRC", "petria", "Hokan ping", "Hokan");
    BridgeEchoService.echoToConfiguredTargets(connectionManager, sourceChannel("SOURCE", "TARGET"), "IRC", "petria", "Hokan: ping", "Hokan");

    assertThat(targetConnection.sentMessages).isEmpty();
  }

  @Test
  void skipsBridgeMessagesWithAndWithoutControlPrefix() {
    ConnectionManager connectionManager = new ConnectionManager();
    RecordingConnection targetConnection = addTarget(connectionManager, "TARGET", "#target");

    BridgeEchoService.echoToConfiguredTargets(connectionManager, sourceChannel("SOURCE", "TARGET"), "Telegram", "petria", "<unh@IRC>: hello", "Hokan");
    BridgeEchoService.echoToConfiguredTargets(connectionManager, sourceChannel("SOURCE", "TARGET"), "Telegram", "petria", "\u0002\u0002<unh@IRC>: hello", "Hokan");

    assertThat(targetConnection.sentMessages).isEmpty();
  }

  @Test
  void skipsSourceAliasTarget() {
    ConnectionManager connectionManager = new ConnectionManager();
    RecordingConnection targetConnection = addTarget(connectionManager, "SOURCE", "#source");

    BridgeEchoService.echoToConfiguredTargets(
        connectionManager,
        sourceChannel("SOURCE", "SOURCE"),
        "IRC",
        "petria",
        "hello",
        "Hokan");

    assertThat(targetConnection.sentMessages).isEmpty();
  }

  @Test
  void emitsPlainBridgeMessageWithoutControlPrefix() {
    assertThat(BridgeEchoService.formatBridgeMessage("Telegram", "petria", "hello"))
        .isEqualTo("<petria@Telegram>: hello");
  }

  @Test
  void treatsNullBlankAndCommandsAsSkipped() {
    assertThat(BridgeEchoService.shouldSkipEcho(null, "Hokan")).isTrue();
    assertThat(BridgeEchoService.shouldSkipEcho(" ", "Hokan")).isTrue();
    assertThat(BridgeEchoService.shouldSkipEcho("!ping", "Hokan")).isTrue();
    assertThat(BridgeEchoService.shouldSkipEcho("hokan: ping", "Hokan")).isTrue();
    assertThat(BridgeEchoService.shouldSkipEcho("hokan ping", "Hokan")).isTrue();
    assertThat(BridgeEchoService.shouldSkipEcho("hokandev ping", "Hokan")).isFalse();
  }

  private Channel sourceChannel(String sourceEchoToAlias, String... targetAliases) {
    return Channel.builder()
        .name("#source")
        .echoToAlias(sourceEchoToAlias)
        .echoToAliases(List.of(targetAliases))
        .build();
  }

  private RecordingConnection addTarget(ConnectionManager connectionManager, String echoToAlias, String channelName) {
    RecordingConnection connection = new RecordingConnection();
    connectionManager.updateJoinedChannelsMap(
        BotConnectionType.IRC_CONNECTION,
        connection,
        new BotConnectionChannel("id-" + echoToAlias, echoToAlias, BotConnectionType.IRC_CONNECTION.name(), "IRCNet", channelName));
    return connection;
  }

  private static class RecordingConnection extends BotConnection {
    private final List<Message> sentMessages = new ArrayList<>();

    private RecordingConnection() {
      super(BotConnectionType.IRC_CONNECTION);
    }

    @Override
    public String getNetwork() {
      return "IRCNet";
    }

    @Override
    public void sendMessageTo(Message message) {
      sentMessages.add(message);
    }
  }
}
