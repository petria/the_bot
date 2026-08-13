package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.ChannelUsersByEchoToAliasResponse;
import org.freakz.common.model.connectionmanager.IrcOperatorModeRequest;
import org.freakz.common.model.connectionmanager.IrcOperatorModeResponse;
import org.freakz.common.model.connectionmanager.IrcTopicSetResponse;
import org.freakz.common.model.connectionmanager.IrcTopicStateResponse;
import org.freakz.common.model.connectionmanager.IrcTopicWebSetRequest;
import org.freakz.common.model.connectionmanager.IrcModeStateResponse;
import org.freakz.common.model.connectionmanager.IrcModeSetResponse;
import org.freakz.common.model.connectionmanager.IrcModeWebSetRequest;
import org.freakz.common.model.engine.livechannel.LiveChannelEventsResponse;
import org.freakz.common.model.engine.livechannel.LiveChannelSendRequest;
import org.freakz.common.model.engine.livechannel.LiveChannelSendResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.web.channels.ChannelAccessService;
import org.freakz.web.config.AdminConnectionConfigService;
import org.freakz.web.config.AdminConnectionConfigService.LiveChannelSettingsApplyResponse;
import org.freakz.web.config.AdminConnectionConfigService.LiveChannelSettingsDto;
import org.freakz.web.livechannels.LiveChannelCatalogService;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
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
  private final ChannelAccessService accessService;
  private final LiveChannelCatalogService catalogService;
  private final AdminConnectionConfigService configService;
  private final HttpClient streamingHttpClient;

  @Autowired
  public AdminLiveChannelsController(
      RestEngineClient engineClient,
      RestConnectionManagerClient connectionManagerClient,
      ChannelAccessService accessService,
      LiveChannelCatalogService catalogService,
      AdminConnectionConfigService configService) {
    this(
        engineClient,
        connectionManagerClient,
        accessService,
        catalogService,
        configService,
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build());
  }

  AdminLiveChannelsController(
      RestEngineClient engineClient,
      RestConnectionManagerClient connectionManagerClient,
      ChannelAccessService accessService,
      LiveChannelCatalogService catalogService,
      AdminConnectionConfigService configService,
      HttpClient streamingHttpClient) {
    this.engineClient = engineClient;
    this.connectionManagerClient = connectionManagerClient;
    this.accessService = accessService;
    this.catalogService = catalogService;
    this.configService = configService;
    this.streamingHttpClient = streamingHttpClient;
  }

  @GetMapping("/channels")
  public LiveChannelsResponse channels(@AuthenticationPrincipal BotUserPrincipal principal) {
    List<LiveChannelResponse> channels = catalogService.publicChannels().stream()
        .filter(channel -> accessService.canView(principal, channel.connectionType(), channel.echoToAlias()))
        .map(channel -> new LiveChannelResponse(
            channel.echoToAlias(),
            channel.label(),
            channel.connectionType(),
            channel.network(),
            channel.channelType(),
            accessService.canSend(principal, channel.connectionType(), channel.echoToAlias()),
            accessService.canAdmin(principal, channel.connectionType(), channel.echoToAlias()),
            "irc".equals(accessService.connectionKey(channel.connectionType()))
                && accessService.canMode(principal, channel.connectionType(), channel.echoToAlias())))
        .toList();
    return new LiveChannelsResponse(channels);
  }

  @GetMapping("/settings")
  public LiveChannelSettingsDto settings(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestParam String echoToAlias) {
    String alias = trim(echoToAlias);
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(alias);
    accessService.requireAdmin(principal, channel.connectionType(), alias);
    return configService.readChannelSettings(channel.connectionType(), channel.network(), alias);
  }

  @PutMapping("/settings")
  public LiveChannelSettingsApplyResponse saveSettings(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody LiveChannelSettingsUpdateRequest request) {
    String alias = trim(request == null ? null : request.echoToAlias());
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(alias);
    accessService.requireAdmin(principal, channel.connectionType(), alias);
    return configService.saveAndApplyChannelSettings(
        channel.connectionType(),
        channel.network(),
        alias,
        new LiveChannelSettingsDto(
            request.publicAiEnabled(),
            request.allowAnonymousAiCommands(),
            request.resolveUrls(),
            request.captureResolvedUrls(),
            request.captureImages()));
  }

  @GetMapping("/topic")
  public LiveChannelTopicResponse topic(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestParam String echoToAlias) {
    String alias = trim(echoToAlias);
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(alias);
    requireIrcChannel(channel);
    accessService.requireView(principal, channel.connectionType(), alias);
    IrcTopicStateResponse state = connectionManagerClient.getIrcTopicStates().stream()
        .filter(item -> alias.equalsIgnoreCase(item.echoToAlias()))
        .findFirst()
        .orElse(null);
    return state == null
        ? new LiveChannelTopicResponse(alias, channel.label(), null, null, false, false, false, false, false)
        : new LiveChannelTopicResponse(
            state.echoToAlias(),
            state.channelName(),
            state.configuredTopic(),
            state.currentTopic(),
            state.manageTopic(),
            state.connected(),
            state.joined(),
            state.mismatch(),
            accessService.canAdmin(principal, channel.connectionType(), alias) && state.manageTopic());
  }

  @PutMapping("/topic")
  public IrcTopicSetResponse saveTopic(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody IrcTopicUpdateRequest request) {
    String alias = trim(request == null ? null : request.echoToAlias());
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(alias);
    requireIrcChannel(channel);
    accessService.requireAdmin(principal, channel.connectionType(), alias);
    if (request.topic() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is required");
    }
    IrcTopicSetResponse response;
    try {
      response = engineClient.setIrcTopicFromWeb(
          new IrcTopicWebSetRequest(alias, request.topic(), principal.getUsername()));
    } catch (RestClientResponseException e) {
      String detail = e.getResponseBodyAsString();
      throw new ResponseStatusException(
          HttpStatus.valueOf(e.getStatusCode().value()),
          detail == null || detail.isBlank() ? "bot-engine rejected the topic update" : detail,
          e);
    } catch (RestClientException e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not update IRC topic through bot-engine", e);
    }
    if (response == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not return a topic result");
    }
    return response;
  }

  @GetMapping("/mode")
  public LiveChannelModeResponse mode(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestParam String echoToAlias) {
    String alias = trim(echoToAlias);
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(alias);
    requireIrcChannel(channel);
    accessService.requireView(principal, channel.connectionType(), alias);
    IrcModeStateResponse state = connectionManagerClient.getIrcModeStates().stream()
        .filter(item -> alias.equalsIgnoreCase(item.echoToAlias()))
        .findFirst()
        .orElse(null);
    return state == null
        ? new LiveChannelModeResponse(alias, channel.label(), null, null, false, false, false, false, false)
        : new LiveChannelModeResponse(
            state.echoToAlias(), state.channelName(), state.configuredModes(), state.currentModes(),
            state.manageMode(), state.connected(), state.joined(), state.mismatch(),
            accessService.canAdmin(principal, channel.connectionType(), alias) && state.manageMode());
  }

  @PutMapping("/mode")
  public IrcModeSetResponse saveMode(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody IrcModeUpdateRequest request) {
    String alias = trim(request == null ? null : request.echoToAlias());
    if (alias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(alias);
    requireIrcChannel(channel);
    accessService.requireAdmin(principal, channel.connectionType(), alias);
    if (request.modes() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modes are required");
    }
    IrcModeSetResponse response;
    try {
      response = engineClient.setIrcModesFromWeb(
          new IrcModeWebSetRequest(alias, request.modes(), principal.getUsername()));
    } catch (RestClientResponseException e) {
      throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()),
          e.getResponseBodyAsString(), e);
    } catch (RestClientException e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not update IRC channel modes through bot-engine", e);
    }
    if (response == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-engine did not return a mode result");
    }
    return response;
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
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(alias);
    accessService.requireView(principal, channel.connectionType(), alias);
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
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(alias);
    accessService.requireView(principal, channel.connectionType(), alias);
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
        HttpRequest request = engineClient.internalRequest(HttpRequest.newBuilder(uri))
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
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(alias);
    accessService.requireView(principal, channel.connectionType(), alias);
    ResponseEntity<ChannelUsersByEchoToAliasResponse> response = connectionManagerClient.getChannelUsersByEchoToAlias(
        new ChannelUsersByEchoToAliasRequest(alias));
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-io did not return channel users");
    }
    return response.getBody();
  }

  @PostMapping("/irc-operator-mode")
  public IrcOperatorModeResponse setIrcOperatorMode(
      @AuthenticationPrincipal BotUserPrincipal principal,
      @RequestBody IrcOperatorModeUpdateRequest request) {
    String echoToAlias = trim(request == null ? null : request.echoToAlias());
    List<String> nicks = request == null || request.nicks() == null ? List.of() : request.nicks().stream()
        .map(this::trim)
        .filter(nick -> !nick.isBlank())
        .distinct()
        .toList();
    if (echoToAlias.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Channel alias is required");
    }
    if (nicks.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one IRC user");
    }
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(echoToAlias);
    if (!"irc".equals(accessService.connectionKey(channel.connectionType()))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IRC operator actions are only available for IRC channels");
    }
    accessService.requireMode(principal, channel.connectionType(), echoToAlias);
    IrcOperatorModeResponse response = connectionManagerClient.setIrcOperatorMode(
        new IrcOperatorModeRequest(echoToAlias, nicks, request.operator()));
    if (response == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "bot-io did not return an IRC operator action result");
    }
    return response;
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
    LiveChannelCatalogService.LiveChannelCatalogItem channel = liveChannel(echoToAlias);
    accessService.requireSend(principal, channel.connectionType(), echoToAlias);
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

  private LiveChannelCatalogService.LiveChannelCatalogItem liveChannel(String echoToAlias) {
    return catalogService.findPublicChannel(echoToAlias)
        .orElseGet(() -> new LiveChannelCatalogService.LiveChannelCatalogItem(
            echoToAlias,
            echoToAlias,
            null,
            null,
            null));
  }

  private void requireIrcChannel(LiveChannelCatalogService.LiveChannelCatalogItem channel) {
    if (!"irc".equals(accessService.connectionKey(channel.connectionType()))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic actions are only available for IRC channels");
    }
  }

  public record LiveChannelsResponse(List<LiveChannelResponse> channels) {
  }

  public record LiveChannelResponse(
      String echoToAlias,
      String label,
      String connectionType,
      String network,
      String channelType,
      boolean sendAllowed,
      boolean adminAllowed,
      boolean modeAllowed) {
  }

  public record LiveChannelSettingsUpdateRequest(
      String echoToAlias,
      boolean publicAiEnabled,
      boolean allowAnonymousAiCommands,
      boolean resolveUrls,
      boolean captureResolvedUrls,
      boolean captureImages) {
  }

  public record IrcOperatorModeUpdateRequest(String echoToAlias, List<String> nicks, boolean operator) {
  }

  public record IrcTopicUpdateRequest(String echoToAlias, String topic) {
  }

  public record IrcModeUpdateRequest(String echoToAlias, String modes) {
  }

  public record LiveChannelTopicResponse(
      String echoToAlias,
      String channelName,
      String configuredTopic,
      String currentTopic,
      boolean manageTopic,
      boolean connected,
      boolean joined,
      boolean mismatch,
      boolean editable) {
  }

  public record LiveChannelModeResponse(
      String echoToAlias,
      String channelName,
      String configuredModes,
      String currentModes,
      boolean manageMode,
      boolean connected,
      boolean joined,
      boolean mismatch,
      boolean editable) {
  }

  public record ErrorResponse(String message, String detail) {
  }

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse badRequest(RuntimeException e) {
    Throwable cause = e.getCause();
    return new ErrorResponse(e.getMessage(), cause == null ? null : cause.getMessage());
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
