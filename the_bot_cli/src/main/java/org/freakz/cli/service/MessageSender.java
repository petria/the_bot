package org.freakz.cli.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageSender {

    @Autowired
    private RestEngineClient engineClient;

    public String sendToServer(String message, String botUser) {


        EngineRequest request
                = EngineRequest.builder()
                .fromChannelId(-1L)
                .timestamp(System.currentTimeMillis())
                .command(message)
                .replyTo("NO_REPLY")
                .fromConnectionId(-1)
                .fromSender(botUser)
                .fromSenderId("NO_SENDER_ID")
                .network("BOT_CLI_CLIENT")
                .echoToAlias("THE_BOT_CLI")
                .build();
        try {
            ResponseEntity<EngineResponse> responseEntity = engineClient.handleEngineRequest(request);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                EngineResponse engineResponse = responseEntity.getBody();
                return engineResponse.getMessage();
            }
        } catch (Exception e) {
            log.error("Unable to send to Engine: {}", e.getMessage());
        }

        return "<error>";
    }

}
