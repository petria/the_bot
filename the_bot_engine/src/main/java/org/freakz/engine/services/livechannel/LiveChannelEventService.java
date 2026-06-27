package org.freakz.engine.services.livechannel;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.livechannel.LiveChannelDirection;
import org.freakz.common.model.engine.livechannel.LiveChannelEvent;
import org.freakz.common.model.engine.livechannel.LiveChannelEventsResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LiveChannelEventService {

  private static final int MAX_EVENTS_PER_CHANNEL = 500;
  private static final long EVENT_TTL_MILLIS = Duration.ofHours(2).toMillis();

  private final AtomicLong nextEventId = new AtomicLong(1);
  private final Map<String, Deque<LiveChannelEvent>> eventsByEchoToAlias = new ConcurrentHashMap<>();
  private final Map<String, List<Consumer<LiveChannelEvent>>> subscribersByEchoToAlias = new ConcurrentHashMap<>();

  public void recordInbound(EngineRequest request) {
    if (!isPublicChannelRequest(request)) {
      return;
    }
    record(
        request.getEchoToAlias(),
        request.getTimestamp(),
        request.getFromSender(),
        request.getFromSenderId(),
        request.getCommand(),
        request.getChatProtocol(),
        request.getNetwork(),
        request.getChatType(),
        request.getChatId(),
        LiveChannelDirection.INBOUND);
  }

  public void recordWebOutbound(String echoToAlias, String webUsername, String message) {
    record(
        echoToAlias,
        System.currentTimeMillis(),
        webUsername + "@web-ui",
        null,
        message,
        "web",
        "BOT_WEB",
        "channel",
        echoToAlias,
        LiveChannelDirection.WEB_OUTBOUND);
  }

  public LiveChannelEventsResponse eventsAfter(String echoToAlias, long afterId) {
    if (echoToAlias == null || echoToAlias.isBlank()) {
      return new LiveChannelEventsResponse(List.of());
    }
    Deque<LiveChannelEvent> events = eventsByEchoToAlias.get(echoToAlias);
    if (events == null) {
      return new LiveChannelEventsResponse(List.of());
    }
    synchronized (events) {
      prune(events);
      List<LiveChannelEvent> matching = new ArrayList<>();
      for (LiveChannelEvent event : events) {
        if (event.id() > afterId) {
          matching.add(event);
        }
      }
      return new LiveChannelEventsResponse(matching);
    }
  }

  private boolean isPublicChannelRequest(EngineRequest request) {
    return request != null
        && !request.isPrivateChannel()
        && request.getEchoToAlias() != null
        && !request.getEchoToAlias().isBlank()
        && request.getNetwork() != null
        && !"BOT_WEB_CONSOLE".equals(request.getNetwork())
        && !"BOT_CLI_CLIENT".equals(request.getNetwork())
        && !"BOT_INTERNAL".equals(request.getNetwork())
        && request.getCommand() != null
        && !request.getCommand().isBlank();
  }

  private void record(
      String echoToAlias,
      long requestId,
      String sender,
      String senderId,
      String message,
      String protocol,
      String network,
      String chatType,
      String chatId,
      LiveChannelDirection direction) {
    if (echoToAlias == null || echoToAlias.isBlank() || message == null || message.isBlank()) {
      return;
    }
    LiveChannelEvent event = new LiveChannelEvent(
        nextEventId.getAndIncrement(),
        requestId,
        System.currentTimeMillis(),
        echoToAlias,
        sender,
        senderId,
        message,
        protocol,
        network,
        chatType,
        chatId,
        direction);
    Deque<LiveChannelEvent> events = eventsByEchoToAlias.computeIfAbsent(echoToAlias, ignored -> new ArrayDeque<>());
    synchronized (events) {
      events.addLast(event);
      prune(events);
    }
    publish(echoToAlias, event);
  }

  public LiveChannelSubscription subscribe(String echoToAlias, long afterId, Consumer<LiveChannelEvent> subscriber) {
    if (echoToAlias == null || echoToAlias.isBlank() || subscriber == null) {
      return new LiveChannelSubscription(() -> {
      });
    }
    String alias = echoToAlias.trim();
    List<LiveChannelEvent> backlog = eventsAfter(alias, afterId).events();
    List<Consumer<LiveChannelEvent>> subscribers = subscribersByEchoToAlias.computeIfAbsent(alias, ignored -> new ArrayList<>());
    synchronized (subscribers) {
      subscribers.add(subscriber);
    }
    backlog.forEach(subscriber);
    return new LiveChannelSubscription(() -> {
      synchronized (subscribers) {
        subscribers.remove(subscriber);
        if (subscribers.isEmpty()) {
          subscribersByEchoToAlias.remove(alias, subscribers);
        }
      }
    });
  }

  private void publish(String echoToAlias, LiveChannelEvent event) {
    List<Consumer<LiveChannelEvent>> subscribers = subscribersByEchoToAlias.get(echoToAlias);
    if (subscribers == null) {
      return;
    }
    List<Consumer<LiveChannelEvent>> snapshot;
    synchronized (subscribers) {
      snapshot = List.copyOf(subscribers);
    }
    snapshot.forEach(subscriber -> {
      try {
        subscriber.accept(event);
      } catch (RuntimeException e) {
        synchronized (subscribers) {
          subscribers.remove(subscriber);
        }
      }
    });
  }

  private void prune(Deque<LiveChannelEvent> events) {
    long oldestAllowed = System.currentTimeMillis() - EVENT_TTL_MILLIS;
    Iterator<LiveChannelEvent> iterator = events.iterator();
    while (iterator.hasNext()) {
      LiveChannelEvent event = iterator.next();
      if (event.createdAt() < oldestAllowed) {
        iterator.remove();
      } else {
        break;
      }
    }
    while (events.size() > MAX_EVENTS_PER_CHANNEL) {
      events.removeFirst();
    }
  }

  public record LiveChannelSubscription(Runnable closeAction) implements AutoCloseable {
    @Override
    public void close() {
      closeAction.run();
    }
  }
}
