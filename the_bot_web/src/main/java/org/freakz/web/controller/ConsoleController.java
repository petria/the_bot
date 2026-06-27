package org.freakz.web.controller;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.engine.console.ConsoleCommandRequest;
import org.freakz.common.model.engine.console.ConsoleCommandResponse;
import org.freakz.common.model.engine.console.ConsoleEventsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/web/console")
public class ConsoleController {

  private static final String CONSOLE_NETWORK = "BOT_WEB_CONSOLE";
  private static final String CONSOLE_ECHO_ALIAS = "THE_BOT_WEB_CONSOLE";
  private static final ExecutorService consoleStreamExecutor = Executors.newCachedThreadPool(runnable -> {
    Thread thread = new Thread(runnable, "console-web-stream");
    thread.setDaemon(true);
    return thread;
  });

  private final RestEngineClient engineClient;
  private final HttpClient streamingHttpClient;

  @Autowired
  public ConsoleController(RestEngineClient engineClient) {
    this(
        engineClient,
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build());
  }

  ConsoleController(RestEngineClient engineClient, HttpClient streamingHttpClient) {
    this.engineClient = engineClient;
    this.streamingHttpClient = streamingHttpClient;
  }

  @PostMapping("/command")
  public ConsoleCommandResponse command(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody ConsoleCommandRequest request) {
    String command = request == null ? "" : trim(request.command());
    String sessionId = request == null ? "" : trim(request.sessionId());
    if (command.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Command is required");
    }
    if (sessionId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session id is required");
    }

    long requestId = System.currentTimeMillis();
    String sessionKey = sessionKey(principal, sessionId);
    EngineRequest engineRequest = EngineRequest.builder()
        .fromChannelId(-1L)
        .timestamp(requestId)
        .command(command)
        .replyTo(sessionKey)
        .fromConnectionId(-1)
        .isPrivateChannel(true)
        .fromSender(principal.getUsername())
        .fromSenderId("WEB-CONSOLE:" + principal.getId())
        .network(CONSOLE_NETWORK)
        .chatProtocol("web")
        .chatType("console")
        .chatId(sessionKey)
        .echoToAlias(CONSOLE_ECHO_ALIAS)
        .build();

    ResponseEntity<EngineResponse> response = engineClient.handleEngineRequest(engineRequest);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not accept console command");
    }
    return new ConsoleCommandResponse(requestId, true);
  }

  @GetMapping("/events")
  public ConsoleEventsResponse events(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestParam String sessionId,
      @RequestParam(defaultValue = "0") long afterId) {
    String trimmedSessionId = trim(sessionId);
    if (trimmedSessionId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session id is required");
    }
    ResponseEntity<ConsoleEventsResponse> response =
        engineClient.getConsoleEvents(sessionKey(principal, trimmedSessionId), afterId);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not return console events");
    }
    return response.getBody();
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> stream(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestParam String sessionId,
      @RequestParam(defaultValue = "0") long afterId) {
    String trimmedSessionId = trim(sessionId);
    if (trimmedSessionId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session id is required");
    }

    URI uri = engineClient.consoleEventStreamUri(sessionKey(principal, trimmedSessionId), afterId);
    SseEmitter emitter = new SseEmitter(0L);
    try {
      emitter.send(SseEmitter.event().comment("connected"));
    } catch (IOException e) {
      emitter.completeWithError(e);
      return consoleStreamResponse(emitter);
    }
    consoleStreamExecutor.execute(() -> bridgeEngineStream(uri, emitter));
    return consoleStreamResponse(emitter);
  }

  private String sessionKey(BotUserPrincipal principal, String sessionId) {
    return "web-console:" + principal.getId() + ":" + sanitizeSessionId(sessionId);
  }

  private String sanitizeSessionId(String sessionId) {
    return trim(sessionId).replaceAll("[^A-Za-z0-9_-]", "_");
  }

  private String trim(String value) {
    return value == null ? "" : value.trim();
  }

  private ResponseEntity<SseEmitter> consoleStreamResponse(SseEmitter emitter) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
        .header("X-Accel-Buffering", "no")
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(emitter);
  }

  private void bridgeEngineStream(URI uri, SseEmitter emitter) {
    HttpRequest request = HttpRequest.newBuilder(uri)
        .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
        .timeout(Duration.ofHours(1))
        .GET()
        .build();
    try {
      HttpResponse<InputStream> response = streamingHttpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        emitter.send(SseEmitter.event().name("error").data("bot-engine did not return console stream"));
        emitter.complete();
        return;
      }
      try (InputStream inputStream = response.body()) {
        forwardSse(inputStream, emitter);
      }
    } catch (IOException | IllegalStateException e) {
      emitter.completeWithError(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      emitter.completeWithError(e);
    }
  }

  private static void forwardSse(InputStream inputStream, SseEmitter emitter) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String id = null;
      String eventName = null;
      StringBuilder data = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isEmpty()) {
          sendBufferedEvent(emitter, id, eventName, data);
          id = null;
          eventName = null;
          data.setLength(0);
          continue;
        }
        if (line.startsWith(":")) {
          emitter.send(SseEmitter.event().comment(line.substring(1)));
          continue;
        }
        if (line.startsWith("id:")) {
          id = line.substring(3).trim();
          continue;
        }
        if (line.startsWith("event:")) {
          eventName = line.substring(6).trim();
          continue;
        }
        if (line.startsWith("data:")) {
          if (!data.isEmpty()) {
            data.append('\n');
          }
          data.append(line.substring(5).stripLeading());
        }
      }
      sendBufferedEvent(emitter, id, eventName, data);
    }
  }

  private static void sendBufferedEvent(SseEmitter emitter, String id, String eventName, StringBuilder data)
      throws IOException {
    if (data.isEmpty()) {
      return;
    }
    SseEmitter.SseEventBuilder event = SseEmitter.event().data(data.toString());
    if (id != null && !id.isBlank()) {
      event.id(id);
    }
    if (eventName != null && !eventName.isBlank()) {
      event.name(eventName);
    }
    emitter.send(event);
  }
}
