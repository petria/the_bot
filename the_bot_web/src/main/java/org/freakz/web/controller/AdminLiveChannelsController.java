package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasResponse;
import org.freakz.common.model.engine.livechannel.LiveChannelEventsResponse;
import org.freakz.common.model.engine.livechannel.LiveChannelSendRequest;
import org.freakz.common.model.engine.livechannel.LiveChannelSendResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@RestController
@RequestMapping("/api/web/admin/live-channels")
public class AdminLiveChannelsController {

  private final RestEngineClient engineClient;
  private final RestConnectionManagerClient connectionManagerClient;
  private final HttpClient streamingHttpClient;

  @Autowired
  public AdminLiveChannelsController(
      RestEngineClient engineClient,
      RestConnectionManagerClient connectionManagerClient) {
    this(
        engineClient,
        connectionManagerClient,
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build());
  }

  AdminLiveChannelsController(
      RestEngineClient engineClient,
      RestConnectionManagerClient connectionManagerClient,
      HttpClient streamingHttpClient) {
    this.engineClient = engineClient;
    this.connectionManagerClient = connectionManagerClient;
    this.streamingHttpClient = streamingHttpClient;
  }

  @GetMapping("/events")
  public LiveChannelEventsResponse events(
      @RequestParam String echoToAlias,
      @RequestParam(defaultValue = "0") long afterId) {
    String alias = trim(echoToAlias);
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    ResponseEntity<LiveChannelEventsResponse> response = engineClient.getLiveChannelEvents(alias, afterId);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not return live channel events");
    }
    return response.getBody();
  }

  @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<StreamingResponseBody> stream(
      @RequestParam String echoToAlias,
      @RequestParam(defaultValue = "0") long afterId) {
    String alias = trim(echoToAlias);
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    URI uri = engineClient.liveChannelEventStreamUri(alias, afterId);
    StreamingResponseBody body = outputStream -> {
      HttpRequest request = HttpRequest.newBuilder(uri)
          .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
          .timeout(Duration.ofHours(1))
          .GET()
          .build();
      try {
        HttpResponse<InputStream> response = streamingHttpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not return live channel stream");
        }
        try (InputStream inputStream = response.body()) {
          inputStream.transferTo(outputStream);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    };
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
        .header("X-Accel-Buffering", "no")
        .contentType(MediaType.TEXT_EVENT_STREAM)
        .body(body);
  }

  @GetMapping("/users")
  public ChannelUsersByEchoToAliasResponse users(@RequestParam String echoToAlias) {
    String alias = trim(echoToAlias);
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
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
}
