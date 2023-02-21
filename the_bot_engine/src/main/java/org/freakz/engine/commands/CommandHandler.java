package org.freakz.engine.commands;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.freakz.clients.MessageSendClient;
import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.common.model.json.feed.Message;
import org.freakz.engine.commands.handlers.AbstractCmd;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommandHandler {

    //    private final ApplicationContext applicationContext;
    private final MessageSendClient messageSendClient;
    private final CommandHandlers commandHandlers;

    public CommandHandler(MessageSendClient messageSendClient) throws InitializeFailedException {
        this.messageSendClient = messageSendClient;
        this.commandHandlers = new CommandHandlers();
    }


    public String handleCommand(EngineRequest request) {
        if (request.getCommand().startsWith("!")) {
            parseAndExecute(request);
        }
        return null;
    }

    @SneakyThrows
    private void parseAndExecute(EngineRequest request) {
        log.debug("Handle request: {}", request);

        String firstWord = request.getCommand().split(" ")[0].toLowerCase();

        AbstractCmd handler = getCommandHandler(firstWord);
        if (handler != null) {

            String reply = handler.executeCommand(request);
            if (reply != null) {
                sendReplyMessage(request, reply);
            }
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


    //    @Async
    private AbstractCmd getCommandHandler(String name) {
        try {
            AbstractCmd handler = this.commandHandlers.getMatchingCommandHandlers(name);
            return handler;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
