package org.freakz.engine.commands;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.freakz.clients.MessageSendClient;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.common.model.json.feed.Message;
import org.freakz.engine.commands.handlers.AbstractCmd;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class CommandHandler {

    private final ApplicationContext applicationContext;
    private final MessageSendClient messageSendClient;

    public CommandHandler(ApplicationContext applicationContext, MessageSendClient messageSendClient) {
        this.applicationContext = applicationContext;
        this.messageSendClient = messageSendClient;
    }


    public void handleCommand(EngineRequest request) {
        if (request.getCommand().startsWith("!")) {
            parseAndExecute(request);
        }
    }

    @SneakyThrows
    private void parseAndExecute(EngineRequest request) {
        log.debug("Handle request!");
        String reply = null;
        Thread.sleep(3000L);
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

    @PostConstruct
    public void init() {
        initCommandHandlers();
    }


    @Async
    void initCommandHandlers() {
        log.debug("Scanning command handlers..");
        Map<String, AbstractCmd> beansOfType = applicationContext.getBeansOfType(AbstractCmd.class);
        log.debug("Found: {}", beansOfType.size());
        int foo = 0;
    }

}
