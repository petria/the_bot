package org.freakz.io.connections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.botconfig.WhatsAppConfig;
import org.freakz.common.model.feed.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WhatsAppConnection extends BotConnection {

  private static final Logger log = LoggerFactory.getLogger(WhatsAppConnection.class);
  private static final String DEFAULT_NETWORK = "WhatsApp";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final EventPublisher publisher;
  private final MediaCaptureService mediaCaptureService;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private ConnectionManager connectionManager;
  private WhatsAppConfig config;
  private String botName;

  public WhatsAppConnection(EventPublisher publisher) {
    this(publisher, null);
  }

  public WhatsAppConnection(EventPublisher publisher, MediaCaptureService mediaCaptureService) {
    super(BotConnectionType.WHATSAPP_CONNECTION);
    this.publisher = publisher;
    this.mediaCaptureService = mediaCaptureService;
  }

  public void init(ConnectionManager connectionManager, String botName, WhatsAppConfig config) {
    this.connectionManager = connectionManager;
    this.config = config;
    this.botName = botName;
    registerConfiguredChannels();
  }

  public void applyConfig(WhatsAppConfig config) {
    this.config = config;
    clearChannels();
    registerConfiguredChannels();
  }

  @Override
  public void applyChannelConfig(TheBotConfig theBotConfig) {
    if (theBotConfig == null || theBotConfig.getWhatsappConfig() == null) {
      return;
    }
    applyConfig(theBotConfig.getWhatsappConfig());
  }

  @Override
  public String getNetwork() {
    if (config != null && config.getNetwork() != null && !config.getNetwork().isBlank()) {
      return config.getNetwork();
    }
    return DEFAULT_NETWORK;
  }

  @Override
  public void sendMessageTo(Message message) {
    String to = firstNonBlank(message.getId(), message.getTarget());
    if (to == null) {
      throw new IllegalArgumentException("Missing WhatsApp message target");
    }
    sendText(to, message.getMessage());
  }

  @Override
  public void sendProcessingIndicator(Message message) {
    String to = firstNonBlank(message.getId(), message.getTarget());
    if (to == null) {
      log.debug("Can not send WhatsApp typing indicator without target");
      return;
    }
    try {
      sendPresence(to, "typing");
    } catch (Exception e) {
      log.debug("WhatsApp typing indicator failed: {}", e.getMessage());
    }
  }

  protected void sendText(String to, String text) {
    String sendBaseUrl = firstNonBlank(config == null ? null : config.getSendBaseUrl(), "http://bot-whatsapp:8095");
    String url = sendBaseUrl.replaceFirst("/+$", "") + "/send";

    Map<String, String> request = new HashMap<>();
    request.put("to", to);
    request.put("message", text);
    try {
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(request)));
      String token = config == null ? null : config.getSendToken();
      if (token != null && !token.isBlank()) {
        requestBuilder.header("X-Bot-Whatsapp-Token", token);
      }
      HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new RuntimeException("WhatsApp sidecar send failed: HTTP " + response.statusCode() + " " + response.body());
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to send WhatsApp message to " + to, e);
    }
  }

  protected void sendPresence(String to, String presence) {
    String sendBaseUrl = firstNonBlank(config == null ? null : config.getSendBaseUrl(), "http://bot-whatsapp:8095");
    String url = sendBaseUrl.replaceFirst("/+$", "") + "/presence";

    Map<String, String> request = new HashMap<>();
    request.put("to", to);
    request.put("presence", presence);
    try {
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(request)));
      String token = config == null ? null : config.getSendToken();
      if (token != null && !token.isBlank()) {
        requestBuilder.header("X-Bot-Whatsapp-Token", token);
      }
      HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new RuntimeException("WhatsApp sidecar presence failed: HTTP " + response.statusCode() + " " + response.body());
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to send WhatsApp presence to " + to, e);
    }
  }

  public void handleWebhook(WacliWebhookMessageEvent event) {
    if (event == null || event.getChatJid() == null || ((event.getText() == null || event.getText().isBlank()) && !event.hasMedia())) {
      log.debug("Ignoring WhatsApp webhook without chat/text/media");
      return;
    }
    if (event.isFromMe()) {
      log.debug("Ignoring own WhatsApp message: {}", event.getMessageId());
      return;
    }

    String echoToAlias = resolveEchoToAlias(event);
    String senderJid = event.effectiveSenderJid();
    String actorName = event.senderDisplayName();
    String channelName = event.isPrivateChat() ? "WhatsApp DM " + actorName : event.getChatJid();

    connectionManager.markMessageReceived(
        echoToAlias,
        actorName,
        "WhatsApp",
        getType().name(),
        getNetwork(),
        channelName);
    connectionManager.markUserSeen(
        this,
        echoToAlias,
        senderJid,
        actorName,
        actorName,
        "WHATSAPP_MESSAGE",
        event.getChatJid(),
        channelName);

    Channel configuredChannel = resolveConfiguredChannel(event.getChatJid());
    if (event.hasMedia()) {
      log.debug("WhatsApp media message received messageId={} chatJid={} configuredChannel={} captureImages={} captureTargets={}",
          event.getMessageId(),
          event.getChatJid(),
          configuredChannel == null ? null : configuredChannel.getEchoToAlias(),
          configuredChannel == null ? null : configuredChannel.getCaptureImages(),
          configuredChannel == null ? null : configuredChannel.getCaptureImageToAliases());
    }
    if (event.getText() != null && !event.getText().isBlank()) {
      publisher.publishEvent(this, event, echoToAlias);
      checkEchoTo(event, actorName);
    }
    if (event.hasMedia() && mediaCaptureService != null) {
      mediaCaptureService.captureAndSend(
          connectionManager,
          configuredChannel,
          this,
          "WhatsApp",
          actorName,
          event.getText(),
          resolveMediaDownloadUrl(event),
          event.getMediaContentType(),
          event.getMediaFileName(),
          mediaDownloadHeaders());
    }
  }

  private String resolveMediaDownloadUrl(WacliWebhookMessageEvent event) {
    if (event.hasDownloadableMediaUrl()) {
      return event.getMediaUrl();
    }
    if (event.getChatJid() == null || event.getMessageId() == null) {
      return null;
    }
    String sendBaseUrl = firstNonBlank(config == null ? null : config.getSendBaseUrl(), "http://bot-whatsapp:8095");
    return sendBaseUrl.replaceFirst("/+$", "")
        + "/media?chat=" + urlEncode(event.getChatJid())
        + "&id=" + urlEncode(event.getMessageId());
  }

  private Map<String, String> mediaDownloadHeaders() {
    String token = config == null ? null : config.getSendToken();
    if (token == null || token.isBlank()) {
      return Map.of();
    }
    return Map.of("X-Bot-Whatsapp-Token", token);
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private void registerConfiguredChannels() {
    if (config == null || config.getChannelList() == null) {
      return;
    }
    for (Channel ch : config.getChannelList()) {
      BotConnectionChannel channel = new BotConnectionChannel();
      channel.setId(ch.getId());
      channel.setNetwork(getNetwork());
      channel.setType(getType().name());
      channel.setName(ch.getName());
      channel.setEchoToAlias(ch.getEchoToAlias());
      channel.setConfigured(true);
      connectionManager.updateJoinedChannelsMap(BotConnectionType.WHATSAPP_CONNECTION, this, channel);
    }
  }

  private String resolveEchoToAlias(WacliWebhookMessageEvent event) {
    Channel configured = resolveConfiguredChannel(event.getChatJid());
    if (configured != null) {
      return configured.getEchoToAlias();
    }
    if (event.isPrivateChat()) {
      return "PRIVATE-WHATSAPP-" + event.getChatJid();
    }
    return "WHATSAPP-" + event.getChatJid();
  }

  private Channel resolveConfiguredChannel(String chatJid) {
    if (config == null || config.getChannelList() == null || chatJid == null) {
      return null;
    }
    for (Channel channel : config.getChannelList()) {
      if (chatJid.equalsIgnoreCase(channel.getId()) || chatJid.equalsIgnoreCase(channel.getName())) {
        return channel;
      }
    }
    return null;
  }

  private void checkEchoTo(WacliWebhookMessageEvent event, String actorName) {
    Channel configured = resolveConfiguredChannel(event.getChatJid());
    BridgeEchoService.echoToConfiguredTargets(
        connectionManager,
        configured,
        "WhatsApp",
        actorName,
        event.getText(),
        botName);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value.trim())) {
        return value.trim();
      }
    }
    return null;
  }
}
