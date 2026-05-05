package org.freakz.web.controller;

import org.freakz.common.model.connectionmanager.BotConnectionResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.common.model.connectionmanager.SendMessageByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.SendMessageByEchoToAliasResponse;
import org.freakz.common.model.connectionmanager.SendMessageToKnownUserRequest;
import org.freakz.common.model.connectionmanager.SendMessageToKnownUserResponse;
import org.freakz.common.model.feed.Message;
import org.freakz.web.config.TheBotWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/web/messages")
public class MessagesController {

  private static final Logger log = LoggerFactory.getLogger(MessagesController.class);

  private final RestTemplate restTemplate;
  private final TheBotWebProperties properties;

  public MessagesController(RestTemplate restTemplate, TheBotWebProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  @PostMapping("/known-user")
  public ResponseEntity<?> sendToKnownUser(@RequestBody SendMessageToKnownUserRequest request) {
    return postToBotIo(
        "/api/hokan/io/messages/send_message_to_known_user",
        request,
        SendMessageToKnownUserResponse.class,
        "Could not send message to known user");
  }

  @PostMapping("/echo-to-alias")
  public ResponseEntity<?> sendByEchoToAlias(@RequestBody SendMessageByEchoToAliasRequest request) {
    return postToBotIo(
        "/api/hokan/io/messages/send_message_by_echo_to_alias",
        request,
        SendMessageByEchoToAliasResponse.class,
        "Could not send message to channel");
  }

  @PostMapping("/irc-private")
  public ResponseEntity<?> sendIrcPrivate(@RequestBody SendIrcPrivateMessageRequest request) {
    if (request.getConnectionId() == null) {
      return ResponseEntity
          .badRequest()
          .body(new SendIrcPrivateMessageResponse("NOK", "Missing IRC connection", null));
    }
    if (request.getNick() == null || request.getNick().isBlank()) {
      return ResponseEntity
          .badRequest()
          .body(new SendIrcPrivateMessageResponse("NOK", "Missing IRC nick", null));
    }
    if (request.getMessage() == null || request.getMessage().isBlank()) {
      return ResponseEntity
          .badRequest()
          .body(new SendIrcPrivateMessageResponse("NOK", "Missing message", null));
    }
    if (!isIrcConnection(request.getConnectionId())) {
      return ResponseEntity
          .badRequest()
          .body(new SendIrcPrivateMessageResponse("NOK", "Selected connection is not an IRC connection", null));
    }

    String nick = request.getNick().trim();
    Message message = Message.builder()
        .target("PRIVATE-" + nick)
        .message(request.getMessage().trim())
        .build();

    String path = "/api/hokan/io/messages/send/" + request.getConnectionId();
    ResponseEntity<?> response = postToBotIo(path, message, String.class, "Could not send IRC private message");
    if (!response.getStatusCode().is2xxSuccessful()) {
      return response;
    }
    return ResponseEntity.ok(new SendIrcPrivateMessageResponse("OK", "Sent IRC private message", nick));
  }

  private boolean isIrcConnection(Integer connectionId) {
    String url = UriComponentsBuilder
        .fromUriString(properties.getBotIoBaseUrl())
        .path("/api/hokan/io/connection_manager/get_connection_map")
        .build()
        .toUriString();

    try {
      GetConnectionMapResponse response = restTemplate.getForObject(url, GetConnectionMapResponse.class);
      BotConnectionResponse connection = response == null || response.getConnectionMap() == null
          ? null
          : response.getConnectionMap().get(connectionId);
      return connection != null && "IRC_CONNECTION".equals(connection.getType());
    } catch (RestClientException e) {
      log.warn("Could not validate IRC connection through bot-io: {}", e.getMessage());
      return false;
    }
  }

  private <T> ResponseEntity<?> postToBotIo(String path, Object request, Class<T> responseType, String errorMessage) {
    String url = UriComponentsBuilder
        .fromUriString(properties.getBotIoBaseUrl())
        .path(path)
        .build()
        .toUriString();

    try {
      T response = restTemplate.postForObject(url, request, responseType);
      return ResponseEntity.ok(response);
    } catch (RestClientException e) {
      log.warn("{} through bot-io: {}", errorMessage, e.getMessage());
      return ResponseEntity
          .status(HttpStatus.BAD_GATEWAY)
          .body(new BotIoProxyErrorResponse(
              "BOT_IO_UNAVAILABLE",
              errorMessage,
              properties.getBotIoBaseUrl(),
              e.getMessage()));
    }
  }

  public static class BotIoProxyErrorResponse {

    private String code;
    private String message;
    private String botIoBaseUrl;
    private String detail;

    public BotIoProxyErrorResponse() {
    }

    public BotIoProxyErrorResponse(String code, String message, String botIoBaseUrl, String detail) {
      this.code = code;
      this.message = message;
      this.botIoBaseUrl = botIoBaseUrl;
      this.detail = detail;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getBotIoBaseUrl() {
      return botIoBaseUrl;
    }

    public void setBotIoBaseUrl(String botIoBaseUrl) {
      this.botIoBaseUrl = botIoBaseUrl;
    }

    public String getDetail() {
      return detail;
    }

    public void setDetail(String detail) {
      this.detail = detail;
    }
  }

  public static class SendIrcPrivateMessageRequest {

    private Integer connectionId;
    private String nick;
    private String message;

    public SendIrcPrivateMessageRequest() {
    }

    public Integer getConnectionId() {
      return connectionId;
    }

    public void setConnectionId(Integer connectionId) {
      this.connectionId = connectionId;
    }

    public String getNick() {
      return nick;
    }

    public void setNick(String nick) {
      this.nick = nick;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }

  public static class SendIrcPrivateMessageResponse {

    private String status;
    private String message;
    private String sentTo;

    public SendIrcPrivateMessageResponse() {
    }

    public SendIrcPrivateMessageResponse(String status, String message, String sentTo) {
      this.status = status;
      this.message = message;
      this.sentTo = sentTo;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getSentTo() {
      return sentTo;
    }

    public void setSentTo(String sentTo) {
      this.sentTo = sentTo;
    }
  }
}
