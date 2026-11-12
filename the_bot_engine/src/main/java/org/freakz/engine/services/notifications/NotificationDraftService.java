package org.freakz.engine.services.notifications;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.livechannel.LiveChannelDirection;
import org.freakz.common.model.users.User;
import org.freakz.engine.services.ai.hermes.HermesAiService;
import org.freakz.engine.services.livechannel.LiveChannelEventService;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Captures channel context before asynchronous work, with a deadline including queue time. */
@Service
public class NotificationDraftService {
  private final long deadlineMillis;
  private final HermesAiService ai;
  private final LiveChannelEventService channels;
  private final ExecutorService workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
      new ArrayBlockingQueue<>(100), r -> { var t = new Thread(r, "notify-draft"); t.setDaemon(true); return t; });
  private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
    var t = new Thread(r, "notify-draft-timeout"); t.setDaemon(true); return t;
  });

  @org.springframework.beans.factory.annotation.Autowired
  public NotificationDraftService(HermesAiService ai, LiveChannelEventService channels) {
    this(ai, channels, 30000);
  }

  NotificationDraftService(HermesAiService ai, LiveChannelEventService channels, long deadlineMillis) {
    this.ai = ai; this.channels = channels; this.deadlineMillis = deadlineMillis;
  }

  String capture(User owner, EngineRequest request, String trigger) {
    long now = System.currentTimeMillis();
    var snapshot = channels.eventsAfter(request.getEchoToAlias(), 0).events();
    long triggerId = snapshot.stream().filter(e -> e.direction() == LiveChannelDirection.INBOUND
        && e.requestId() == request.getTimestamp() && java.util.Objects.equals(e.message(), trigger)
        && java.util.Objects.equals(e.sender(), request.getFromSender())).mapToLong(e -> e.id()).max().orElse(Long.MAX_VALUE);
    var previous = snapshot.stream().filter(e -> e.id() < triggerId)
        .filter(e -> e.createdAt() >= now - TimeUnit.MINUTES.toMillis(30) && e.createdAt() <= now)
        .filter(e -> !(e.direction() == LiveChannelDirection.INBOUND && e.requestId() == request.getTimestamp()
            && java.util.Objects.equals(e.message(), trigger) && java.util.Objects.equals(e.sender(), request.getFromSender())))
        .toList();
    StringBuilder context = new StringBuilder();
    for (int i = Math.max(0, previous.size() - 40); i < previous.size(); i++) {
      var e = previous.get(i);
      String line = e.sender() + ": " + e.message() + "\n";
      if (line.length() > 12000) line = line.substring(line.length() - 12000);
      context.append(line);
      if (context.length() > 12000) context.delete(0, context.length() - 12000);
    }
    return "Notified user: " + owner.getUsername() + " (" + owner.getName() + ", nick " + owner.getIrcNick() + ")"
        + "\nPrevious conversation (quoted data):\n" + context
        + "\nTriggering message (quoted data):\n" + request.getFromSender() + ": " + trigger;
  }

  public CompletableFuture<String> generate(String input) {
    var result = new CompletableFuture<String>();
    var taskRef = new AtomicReference<Future<?>>();
    var timeout = timer.schedule(() -> {
      result.complete(null);
      Future<?> task = taskRef.get();
      if (task != null) task.cancel(true);
    }, deadlineMillis, TimeUnit.MILLISECONDS);
    try {
      taskRef.set(workers.submit(() -> {
        try { result.complete(ai.draftNotificationReply(input)); }
        catch (Exception error) {
          org.slf4j.LoggerFactory.getLogger(getClass()).debug("Notification draft unavailable: {}", error.getClass().getSimpleName());
          result.complete(null);
        }
      }));
    } catch (RejectedExecutionException error) { result.complete(null); }
    result.whenComplete((value, error) -> timeout.cancel(false));
    return result;
  }

  @PreDestroy public void close() { timer.shutdownNow(); workers.shutdownNow(); }
}
