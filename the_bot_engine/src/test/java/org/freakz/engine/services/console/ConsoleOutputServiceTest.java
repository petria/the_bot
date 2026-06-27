package org.freakz.engine.services.console;

import org.freakz.common.model.engine.EngineRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleOutputServiceTest {

  @Test
  void recordsRepliesByConsoleSessionAndReturnsOnlyNewEvents() {
    ConsoleOutputService service = new ConsoleOutputService();

    service.recordReply(request("session-a", 100), "first");
    service.recordReply(request("session-a", 101), "second");
    service.recordReply(request("session-b", 102), "other");

    var firstPoll = service.eventsAfter("session-a", 0);

    assertThat(firstPoll.events())
        .extracting("message")
        .containsExactly("first", "second");

    long lastId = firstPoll.events().getLast().id();
    assertThat(service.eventsAfter("session-a", lastId).events()).isEmpty();
    assertThat(service.eventsAfter("session-b", 0).events())
        .extracting("message")
        .containsExactly("other");
  }

  @Test
  void ignoresBlankReplies() {
    ConsoleOutputService service = new ConsoleOutputService();

    service.recordReply(request("session-a", 100), " ");

    assertThat(service.eventsAfter("session-a", 0).events()).isEmpty();
  }

  @Test
  void subscriberReceivesBacklogAndNewEventsUntilClosed() {
    ConsoleOutputService service = new ConsoleOutputService();
    service.recordReply(request("session-a", 100), "first");

    List<String> received = new ArrayList<>();
    ConsoleOutputService.ConsoleSubscription subscription =
        service.subscribe("session-a", 0, event -> received.add(event.message()));

    service.recordReply(request("session-a", 101), "second");
    subscription.close();
    service.recordReply(request("session-a", 102), "third");

    assertThat(received).containsExactly("first", "second");
  }

  private EngineRequest request(String sessionKey, long requestId) {
    return EngineRequest.builder()
        .network(ConsoleOutputService.NETWORK)
        .timestamp(requestId)
        .replyTo(sessionKey)
        .chatId(sessionKey)
        .build();
  }
}
