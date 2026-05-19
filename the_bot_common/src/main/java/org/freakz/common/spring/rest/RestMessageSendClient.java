package org.freakz.common.spring.rest;

import org.freakz.common.model.connectionmanager.SendIrcRawMessageByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.SendIrcRawMessageByEchoToAliasResponse;
import org.freakz.common.model.connectionmanager.SendMessageByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.SendMessageByEchoToAliasResponse;
import org.freakz.common.model.connectionmanager.SendMessageToKnownUserRequest;
import org.freakz.common.model.connectionmanager.SendMessageToKnownUserResponse;
import org.freakz.common.model.feed.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestMessageSendClient {

  private static final Logger log = LoggerFactory.getLogger(RestMessageSendClient.class);
  private final RestTemplate restTemplate;
  private final String baseUrl;

  @Autowired
  public RestMessageSendClient(
      RestTemplate restTemplate,
      @Value("${the.bot.rest.bot-io-base-url:http://bot-io:8090}") String botIoBaseUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl = trimTrailingSlash(botIoBaseUrl) + "/api/hokan/io/messages";
  }

  public ResponseEntity<String> sendMessage(int connectionId, Message message) {
    String url = baseUrl + "/send/" + connectionId;
    try {
      return restTemplate.postForEntity(url, message, String.class);
    } catch (Exception e) {
      log.error("Error sending message", e);
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  public ResponseEntity<String> sendProcessingIndicator(int connectionId, Message message) {
    String url = baseUrl + "/processing/" + connectionId;
    try {
      return restTemplate.postForEntity(url, message, String.class);
    } catch (Exception e) {
      log.debug("Error sending processing indicator: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  public ResponseEntity<SendMessageByEchoToAliasResponse> sendMessageByEchoToAlias(SendMessageByEchoToAliasRequest request) {
    String url = baseUrl + "/send_message_by_echo_to_alias";
    try {
      return restTemplate.postForEntity(url, request, SendMessageByEchoToAliasResponse.class);
    } catch (Exception e) {
      log.error("Error sending message by echoToAlias: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(new SendMessageByEchoToAliasResponse());
    }
  }

  public ResponseEntity<SendIrcRawMessageByEchoToAliasResponse> sendIrcRawMessageByEchoToAlias(SendIrcRawMessageByEchoToAliasRequest request) {
    String url = baseUrl + "/send_irc_raw_message_by_echo_to_alias";
    try {
      return restTemplate.postForEntity(url, request, SendIrcRawMessageByEchoToAliasResponse.class);
    } catch (Exception e) {
      log.error("Error sending irc raw message by echoToAlias: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(new SendIrcRawMessageByEchoToAliasResponse());
    }
  }

  public ResponseEntity<SendMessageToKnownUserResponse> sendMessageToKnownUser(SendMessageToKnownUserRequest request) {
    String url = baseUrl + "/send_message_to_known_user";
    try {
      return restTemplate.postForEntity(url, request, SendMessageToKnownUserResponse.class);
    } catch (Exception e) {
      log.error("Error sending message to known user: {}", e.getMessage());
      SendMessageToKnownUserResponse response = new SendMessageToKnownUserResponse();
      response.setStatus("NOK");
      response.setMessage(e.getMessage());
      return ResponseEntity.internalServerError().body(response);
    }
  }

  private String trimTrailingSlash(String value) {
    return value == null ? "" : value.replaceFirst("/+$", "");
  }
}
