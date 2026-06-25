package org.freakz.engine.services.console;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.console.ConsoleEvent;
import org.freakz.common.model.engine.console.ConsoleEventsResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ConsoleOutputService {

  public static final String NETWORK = "BOT_WEB_CONSOLE";
  private static final int MAX_EVENTS_PER_SESSION = 500;
  private static final long EVENT_TTL_MILLIS = Duration.ofHours(2).toMillis();

  private final AtomicLong nextEventId = new AtomicLong(1);
  private final Map<String, Deque<ConsoleEvent>> eventsBySession = new ConcurrentHashMap<>();

  public void recordReply(EngineRequest request, String message) {
    if (request == null || message == null || message.isBlank()) {
      return;
    }
    String sessionKey = sessionKey(request);
    if (sessionKey.isBlank()) {
      return;
    }
    ConsoleEvent event = new ConsoleEvent(
        nextEventId.getAndIncrement(),
        request.getTimestamp(),
        System.currentTimeMillis(),
        message);
    Deque<ConsoleEvent> events = eventsBySession.computeIfAbsent(sessionKey, ignored -> new ArrayDeque<>());
    synchronized (events) {
      events.addLast(event);
      prune(events);
    }
  }

  public ConsoleEventsResponse eventsAfter(String sessionKey, long afterId) {
    if (sessionKey == null || sessionKey.isBlank()) {
      return new ConsoleEventsResponse(List.of());
    }
    Deque<ConsoleEvent> events = eventsBySession.get(sessionKey);
    if (events == null) {
      return new ConsoleEventsResponse(List.of());
    }
    synchronized (events) {
      prune(events);
      List<ConsoleEvent> matching = new ArrayList<>();
      for (ConsoleEvent event : events) {
        if (event.id() > afterId) {
          matching.add(event);
        }
      }
      return new ConsoleEventsResponse(matching);
    }
  }

  private String sessionKey(EngineRequest request) {
    String chatId = request.getChatId();
    if (chatId != null && !chatId.isBlank()) {
      return chatId;
    }
    return request.getReplyTo() == null ? "" : request.getReplyTo();
  }

  private void prune(Deque<ConsoleEvent> events) {
    long oldestAllowed = System.currentTimeMillis() - EVENT_TTL_MILLIS;
    Iterator<ConsoleEvent> iterator = events.iterator();
    while (iterator.hasNext()) {
      ConsoleEvent event = iterator.next();
      if (event.createdAt() < oldestAllowed) {
        iterator.remove();
      } else {
        break;
      }
    }
    while (events.size() > MAX_EVENTS_PER_SESSION) {
      events.removeFirst();
    }
  }
}
