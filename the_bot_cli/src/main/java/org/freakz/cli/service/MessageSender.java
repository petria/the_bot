package org.freakz.cli.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.cli.clients.EngineClient;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.util.FeignUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class MessageSender {

    @Autowired
    private EngineClient engineClient;

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
            Response response = engineClient.handleEngineRequest(request);
            if (response.status() != 200) {
                log.error("{}: Engine not running: {}", response.status(), response.reason());
            } else {
                Optional<EngineResponse> responseBody = FeignUtils.getResponseBody(response, EngineResponse.class, new ObjectMapper());
                if (responseBody.isPresent()) {
                    EngineResponse engineResponse = responseBody.get();
//                    log.debug("EngineResponse: {}", engineResponse);
                    return engineResponse.getMessage();
                } else {
                    log.error("No EngineResponse!?");
                }
            }
        } catch (Exception e) {
            log.error("Unable to send to Engine: {}", e.getMessage());
        }

        return "<error>";
    }

}
