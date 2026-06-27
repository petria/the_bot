package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasResponse;
import org.freakz.common.model.engine.livechannel.LiveChannelEventsResponse;
import org.freakz.common.model.engine.livechannel.LiveChannelSendRequest;
import org.freakz.common.model.engine.livechannel.LiveChannelSendResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.web.livechannels.LiveChannelAccessService;
import org.freakz.web.livechannels.LiveChannelCatalogService;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/web/live-channels")
public class AdminLiveChannelsController {

  private static final ExecutorService liveChannelStreamExecutor = Executors.newCachedThreadPool(runnable -> {
    Thread thread = new Thread(runnable, "live-channel-web-stream");
    thread.setDaemon(true);
    return thread;
  });

  private final RestEngineClient engineClient;
  private final RestConnectionManagerClient connectionManagerClient;
  private final LiveChannelAccessService accessService;
  private final LiveChannelCatalogService catalogService;
  private final HttpClient streamingHttpClient;

  @Autowired
  public AdminLiveChannelsController(
      RestEngineClient engineClient,
      RestConnectionManagerClient connectionManagerClient,
      LiveChannelAccessService accessService,
      LiveChannelCatalogService catalogService) {
    this(
        engineClient,
        connectionManagerClient,
        accessService,
        catalogService,
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build());
  }

  AdminLiveChannelsController(
      RestEngineClient engineClient,
      RestConnectionManagerClient connectionManagerClient,
      LiveChannelAccessService accessService,
      LiveChannelCatalogService catalogService,
      HttpClient streamingHttpClient) {
    this.engineClient = engineClient;
    this.connectionManagerClient = connectionManagerClient;
    this.accessService = accessService;
    this.catalogService = catalogService;
    this.streamingHttpClient = streamingHttpClient;
  }

  @GetMapping("/channels")
  public LiveChannelsResponse channels(@AuthenticationPrincipal BotUserPrincipal principal) {
    List<LiveChannelResponse> channels = catalogService.publicChannels().stream()
        .filter(channel -> accessService.canView(principal, channel.echoToAlias()))
        .map(channel -> new LiveChannelResponse(
            channel.echoToAlias(),
            channel.label(),
            channel.connectionType(),
            channel.network(),
            channel.channelType(),
            accessService.canSend(principal, channel.echoToAlias())))
        .toList();
    return new LiveChannelsResponse(channels);
  }

  @GetMapping("/events")
  public LiveChannelEventsResponse events(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestParam String echoToAlias,
      @RequestParam(defaultValue = "0") long afterId) {
    String alias = trim(echoToAlias);
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    accessService.requireView(principal, alias);
    ResponseEntity<LiveChannelEventsResponse> response = engineClient.getLiveChannelEvents(alias, afterId);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not return live channel events");
    }
    return response.getBody();
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> stream(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestParam String echoToAlias,
      @RequestParam(defaultValue = "0") long afterId) {
    String alias = trim(echoToAlias);
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    accessService.requireView(principal, alias);
    URI uri = engineClient.liveChannelEventStreamUri(alias, afterId);
    SseEmitter emitter = new SseEmitter(0L);
    try {
      emitter.send(SseEmitter.event().comment("connected"));
    } catch (IOException e) {
      emitter.completeWithError(e);
      return ResponseEntity.ok()
          .header(HttpHeaders.CACHE_CONTROL, "no-cache")
          .header("X-Accel-Buffering", "no")
          .contentType(MediaType.TEXT_EVENT_STREAM)
          .body(emitter);
    }
    liveChannelStreamExecutor.execute(() -> {
      HttpRequest request = HttpRequest.newBuilder(uri)
          .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
          .timeout(Duration.ofHours(1))
          .GET()
          .build();
      try {
        HttpResponse<InputStream> response = streamingHttpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          emitter.send(SseEmitter.event().name("error").data("bot-engine did not return live channel stream"));
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
    });
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
        .header("X-Accel-Buffering", "no")
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(emitter);
  }

  @GetMapping("/users")
  public ChannelUsersByEchoToAliasResponse users(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestParam String echoToAlias) {
    String alias = trim(echoToAlias);
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    accessService.requireView(principal, alias);
    ResponseEntity<ChannelUsersByEchoToAliasResponse> response = connectionManagerClient.getChannelUsersByEchoToAlias(
        new ChannelUsersByEchoToAliasRequest(alias));
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-io did not return channel users");
    }
    return response.getBody();
  }

  @PostMapping("/send")
  public LiveChannelSendResponse send(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody LiveChannelSendRequest request) {
    String echoToAlias = trim(request == null ? null : request.echoToAlias());
    String message = trim(request == null ? null : request.message());
    if (echoToAlias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    if (message.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required");
    }
    accessService.requireSend(principal, echoToAlias);
    ResponseEntity<LiveChannelSendResponse> response = engineClient.sendLiveChannelMessage(
        new LiveChannelSendRequest(echoToAlias, principal.getUsername(), message));
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send live channel message");
    }
    return response.getBody();
  }

  private String trim(String value) {
    return value == null ? "" : value.trim();
  }

  public record LiveChannelsResponse(List<LiveChannelResponse> channels) {
  }

  public record LiveChannelResponse(
      String echoToAlias,
      String label,
      String connectionType,
      String network,
      String channelType,
      boolean sendAllowed) {
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
