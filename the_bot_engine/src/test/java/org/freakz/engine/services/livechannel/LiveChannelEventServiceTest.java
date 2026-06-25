package org.freakz.engine.services.livechannel;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.livechannel.LiveChannelDirection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiveChannelEventServiceTest {

  @Test
  void recordsPublicChannelMessagesByEchoToAliasAndReturnsOnlyNewEvents() {
    LiveChannelEventService service = new LiveChannelEventService();

    service.recordInbound(request("IRC-HOKANDEV", "petria", "hello", 100, false));
    service.recordInbound(request("IRC-HOKANDEV", "other", "!ping", 101, false));
    service.recordInbound(request("IRC-OTHER", "someone", "other channel", 102, false));

    var firstPoll = service.eventsAfter("IRC-HOKANDEV", 0);

    assertThat(firstPoll.events())
        .extracting("sender")
        .containsExactly("petria", "other");
    assertThat(firstPoll.events())
        .extracting("message")
        .containsExactly("hello", "!ping");
    assertThat(firstPoll.events())
        .extracting("direction")
        .containsOnly(LiveChannelDirection.INBOUND);

    long lastId = firstPoll.events().getLast().id();
    assertThat(service.eventsAfter("IRC-HOKANDEV", lastId).events()).isEmpty();
    assertThat(service.eventsAfter("IRC-OTHER", 0).events())
        .extracting("message")
        .containsExactly("other channel");
  }

  @Test
  void ignoresPrivateAndInternalRequests() {
    LiveChannelEventService service = new LiveChannelEventService();

    service.recordInbound(request("IRC-HOKANDEV", "petria", "private", 100, true));
    service.recordInbound(request("IRC-HOKANDEV", "petria", "console", 101, false, "BOT_WEB_CONSOLE"));
    service.recordInbound(request(null, "petria", "missing alias", 102, false));

    assertThat(service.eventsAfter("IRC-HOKANDEV", 0).events()).isEmpty();
  }

  @Test
  void recordsWebOutboundMessages() {
    LiveChannelEventService service = new LiveChannelEventService();

    service.recordWebOutbound("IRC-HOKANDEV", "petria", "petria@web-ui>: hello");

    assertThat(service.eventsAfter("IRC-HOKANDEV", 0).events())
        .singleElement()
        .satisfies(event -> {
          assertThat(event.sender()).isEqualTo("petria@web-ui");
          assertThat(event.message()).isEqualTo("petria@web-ui>: hello");
          assertThat(event.direction()).isEqualTo(LiveChannelDirection.WEB_OUTBOUND);
        });
  }

  private EngineRequest request(String echoToAlias, String sender, String message, long timestamp, boolean privateChannel) {
    return request(echoToAlias, sender, message, timestamp, privateChannel, "IRCNet");
  }

  private EngineRequest request(
      String echoToAlias,
      String sender,
      String message,
      long timestamp,
      boolean privateChannel,
      String network) {
    return EngineRequest.builder()
        .timestamp(timestamp)
        .command(message)
        .replyTo("#hokandev")
        .isPrivateChannel(privateChannel)
        .fromSender(sender)
        .fromSenderId(sender + "!user@host")
        .network(network)
        .chatProtocol("irc")
        .chatType(privateChannel ? "dm" : "channel")
        .chatId("irc/ircnet/channel/#hokandev")
        .echoToAlias(echoToAlias)
        .build();
  }
}
