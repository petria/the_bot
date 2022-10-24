package org.freakz.engine.commands;

import lombok.extern.slf4j.Slf4j;
import org.freakz.clients.MessageSendClient;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.common.model.json.feed.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class CommandHandler {

    private final MessageSendClient messageSendClient;

    public CommandHandler(MessageSendClient messageSendClient) {
        this.messageSendClient = messageSendClient;
    }


    public void handleCommand(EngineRequest request) {
        if (request.getCommand().startsWith("!")) {
            parseAndExecute(request);
        }
    }

    private void parseAndExecute(EngineRequest request) {
        String reply = null;
        if (request.getCommand().equals("!ping")) {
            reply = "Pong " + System.currentTimeMillis();
        } else if (request.getCommand().equals("!date")) {
            reply = "Date: " + LocalDateTime.now();
        }

        if (reply != null) {
            sendReplyMessage(request, reply);
        }

    }

    private void sendReplyMessage(EngineRequest request, String reply) {
        Message message
                = Message.builder()
                .sender("BotName")
                .message(reply)
                .target(request.getReplyTo())
                .build();

        try {
            messageSendClient.sendMessage(request.getFromConnectionId(), message);
        } catch (Exception ex) {
            log.error("Sending reply failed: {}", ex.getMessage());
        }
    }


}
