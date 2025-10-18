package org.freakz.engine.clients;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.connectionmanager.SendIrcRawMessageByTargetAliasRequest;
import org.freakz.common.model.connectionmanager.SendMessageByTargetAliasRequest;
import org.freakz.common.model.feed.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class RestMessageSendClient {

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

    public ResponseEntity<String> sendMessageByTargetAlias(SendMessageByTargetAliasRequest request) {
        String url = BASE_URL + "/send_message_by_target_alias";
        try {
            return restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            log.error("Error sending message by target alias: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    public ResponseEntity<String> sendIrcRawMessageByTargetAlias(SendIrcRawMessageByTargetAliasRequest request) {
        String url = BASE_URL + "/send_irc_raw_message_by_target_alias";
        try {
            return restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            log.error("Error sending irc raw message by target alias: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
