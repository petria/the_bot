package org.freakz.web.system;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.freakz.common.model.system.SystemStatusResponse;
import org.freakz.web.controller.SystemController;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SystemStatusStreamService {

  private static final long SAMPLE_INTERVAL_SECONDS = 5L;

  private final SystemController systemController;
  private final AtomicReference<SystemStatusResponse> latest = new AtomicReference<>();
  private final AtomicLong eventId = new AtomicLong();
  private final List<SseEmitter> subscribers = new CopyOnWriteArrayList<>();
  private final ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor(runnable -> {
    Thread thread = new Thread(runnable, "system-status-sampler");
    thread.setDaemon(true);
    return thread;
  });

  public SystemStatusStreamService(SystemController systemController) {
    this.systemController = systemController;
  }

  @PostConstruct
  public void start() {
    sampler.scheduleWithFixedDelay(this::sampleAndBroadcastSafely, 0, SAMPLE_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    sampler.shutdownNow();
    for (SseEmitter subscriber : subscribers) {
      subscriber.complete();
    }
    subscribers.clear();
  }

  public SystemStatusResponse latestOrSample() {
    SystemStatusResponse current = latest.get();
    if (current != null) {
      return current;
    }
    return sample();
  }

  public SystemStatusResponse refreshNow() {
    SystemStatusResponse status = sample();
    broadcast(status);
    return status;
  }

  public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(0L);
    subscribers.add(emitter);
    emitter.onCompletion(() -> subscribers.remove(emitter));
    emitter.onTimeout(() -> subscribers.remove(emitter));
    emitter.onError(ignored -> subscribers.remove(emitter));
    try {
      emitter.send(SseEmitter.event().comment("connected"));
      sendStatus(emitter, latestOrSample());
    } catch (IOException e) {
      subscribers.remove(emitter);
      emitter.completeWithError(e);
    }
    return emitter;
  }

  private void sampleAndBroadcastSafely() {
    try {
      refreshNow();
    } catch (RuntimeException ignored) {
      // Keep the stream alive. The existing status calculation stores component-level errors.
    }
  }

  private SystemStatusResponse sample() {
    SystemStatusResponse status = systemController.getStatus();
    latest.set(status);
    return status;
  }

  private void broadcast(SystemStatusResponse status) {
    for (SseEmitter subscriber : subscribers) {
      try {
        sendStatus(subscriber, status);
      } catch (IOException | IllegalStateException e) {
        subscribers.remove(subscriber);
        subscriber.completeWithError(e);
      }
    }
  }

  private void sendStatus(SseEmitter emitter, SystemStatusResponse status) throws IOException {
    emitter.send(SseEmitter.event()
        .id(Long.toString(eventId.incrementAndGet()))
        .name("status")
        .data(status));
  }
}
