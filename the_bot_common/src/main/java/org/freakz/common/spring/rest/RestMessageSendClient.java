package org.freakz.common.spring.rest;

import org.freakz.common.model.connectionmanager.SendIrcRawMessageByTargetAliasRequest;
import org.freakz.common.model.connectionmanager.SendIrcRawMessageByTargetAliasResponse;
import org.freakz.common.model.connectionmanager.SendMessageByTargetAliasRequest;
import org.freakz.common.model.connectionmanager.SendMessageByTargetAliasResponse;
import org.freakz.common.model.feed.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestMessageSendClient {

  private static final Logger log = LoggerFactory.getLogger(RestMessageSendClient.class);
  private final RestTemplate restTemplate;
  private final String BASE_URL = "http://bot-io:8090/api/hokan/io/messages";

  @Autowired
  public RestMessageSendClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public ResponseEntity<String> sendMessage(int connectionId, Message message) {
    String url = BASE_URL + "/send/" + connectionId;
    try {
      return restTemplate.postForEntity(url, message, String.class);
    } catch (Exception e) {
      log.error("Error sending message: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  public ResponseEntity<SendMessageByTargetAliasResponse> sendMessageByTargetAlias(SendMessageByTargetAliasRequest request) {
    String url = BASE_URL + "/send_message_by_target_alias";
    try {
      return restTemplate.postForEntity(url, request, SendMessageByTargetAliasResponse.class);
    } catch (Exception e) {
      log.error("Error sending message by target alias: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(new SendMessageByTargetAliasResponse());
    }
  }

  public ResponseEntity<SendIrcRawMessageByTargetAliasResponse> sendIrcRawMessageByTargetAlias(SendIrcRawMessageByTargetAliasRequest request) {
    String url = BASE_URL + "/send_irc_raw_message_by_target_alias";
    try {
      return restTemplate.postForEntity(url, request, SendIrcRawMessageByTargetAliasResponse.class);
    } catch (Exception e) {
      log.error("Error sending irc raw message by target alias: {}", e.getMessage());
      return ResponseEntity.internalServerError().body(new SendIrcRawMessageByTargetAliasResponse());
    }
  }
}
