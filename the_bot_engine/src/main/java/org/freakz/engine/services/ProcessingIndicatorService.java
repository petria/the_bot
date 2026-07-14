package org.freakz.engine.services;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
import org.freakz.common.spring.rest.RestMessageSendClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Keeps ephemeral chat typing indicators alive while asynchronous work is running. */
@Service
public class ProcessingIndicatorService {

  private static final Logger log = LoggerFactory.getLogger(ProcessingIndicatorService.class);
  private static final long REFRESH_SECONDS = 4;
  private static final long MAX_DURATION_SECONDS = 10 * 60;

  private final RestMessageSendClient messageSendClient;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, runnable -> {
    Thread thread = new Thread(runnable, "processing-indicator");
    thread.setDaemon(true);
    return thread;
  });
  private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();

  public ProcessingIndicatorService(RestMessageSendClient messageSendClient) {
    this.messageSendClient = messageSendClient;
  }

  public void start(EngineRequest request, String botName) {
    if (!isSupported(request)) {
      return;
    }
    stop(request);
    sendStart(request, botName);

    Session session = new Session(request, botName, System.nanoTime());
    sessions.put(requestKey(request), session);
    session.refreshFuture = scheduler.scheduleAtFixedRate(
        () -> refresh(session),
        REFRESH_SECONDS,
        REFRESH_SECONDS,
        TimeUnit.SECONDS);
  }

  public void stop(EngineRequest request) {
    if (request == null) {
      return;
    }
    Session session = sessions.remove(requestKey(request));
    if (session != null && session.refreshFuture != null) {
      session.refreshFuture.cancel(false);
    }
    if (session != null && supportsExplicitStop(request)) {
      sendStop(request, session.botName);
    }
  }

  private void refresh(Session session) {
    if (elapsedSeconds(session.startedAt) >= MAX_DURATION_SECONDS) {
      stop(session.request);
      return;
    }
    sendStart(session.request, session.botName);
  }

  private void sendStart(EngineRequest request, String botName) {
    try {
      messageSendClient.sendProcessingIndicator(request.getFromConnectionId(), message(request, botName));
    } catch (Exception e) {
      log.debug("Processing indicator start failed: {}", e.getMessage());
    }
  }

  private void sendStop(EngineRequest request, String botName) {
    try {
      messageSendClient.stopProcessingIndicator(request.getFromConnectionId(), message(request, botName));
    } catch (Exception e) {
      log.debug("Processing indicator stop failed: {}", e.getMessage());
    }
  }

  private Message message(EngineRequest request, String botName) {
    return Message.builder()
        .sender(botName)
        .timestamp(System.currentTimeMillis())
        .time(LocalDateTime.now())
        .requestTimestamp(request.getTimestamp())
        .message("processing")
        .messageSource(MessageSource.NONE)
        .target(request.getReplyTo())
        .id(request.getFromChannelId() == null ? null : "" + request.getFromChannelId())
        .build();
  }

  private boolean isSupported(EngineRequest request) {
    if (request == null || request.getFromConnectionId() < 0) {
      return false;
    }
    String protocol = protocol(request);
    return "whatsapp".equals(protocol) || "telegram".equals(protocol) || "discord".equals(protocol);
  }

  private boolean supportsExplicitStop(EngineRequest request) {
    return "whatsapp".equals(protocol(request));
  }

  private String protocol(EngineRequest request) {
    String protocol = request == null ? null : request.getChatProtocol();
    return protocol == null ? "" : protocol.trim().toLowerCase(Locale.ROOT);
  }

  private String requestKey(EngineRequest request) {
    if (request.getRequestId() != null && !request.getRequestId().isBlank()) {
      return request.getRequestId();
    }
    return Integer.toHexString(System.identityHashCode(request));
  }

  private long elapsedSeconds(long startedAt) {
    return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedAt);
  }

  @PreDestroy
  public void shutdown() {
    sessions.values().forEach(session -> {
      if (session.refreshFuture != null) {
        session.refreshFuture.cancel(false);
      }
    });
    scheduler.shutdownNow();
  }

  private static final class Session {
    private final EngineRequest request;
    private final String botName;
    private final long startedAt;
    private volatile ScheduledFuture<?> refreshFuture;

    private Session(EngineRequest request, String botName, long startedAt) {
      this.request = request;
      this.botName = botName;
      this.startedAt = startedAt;
    }
  }
}
