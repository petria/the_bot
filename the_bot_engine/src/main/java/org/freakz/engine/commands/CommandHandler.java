package org.freakz.engine.commands;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.freakz.clients.MessageSendClient;
import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.common.model.json.feed.Message;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.api.HokanCmd;
import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.services.HokanServices;
import org.freakz.services.wholelinetricker.WholeLineTriggers;
import org.freakz.services.wholelinetricker.WholeLineTriggersImpl;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommandHandler {

    //    private final ApplicationContext applicationContext;
    private final MessageSendClient messageSendClient;
    private final CommandHandlerLoader commandHandlerLoader;
    @Getter
    private final HokanServices hokanServices;

    public CommandHandler(MessageSendClient messageSendClient, HokanServices hokanServices) throws InitializeFailedException {
        this.messageSendClient = messageSendClient;
        this.hokanServices = hokanServices;
        this.commandHandlerLoader = new CommandHandlerLoader();
    }

    private WholeLineTriggers wholeLineTriggers = new WholeLineTriggersImpl(this);

    public String handleCommand(EngineRequest request) {
        wholeLineTriggers.checkWholeLineTrigger(request);

        if (request.getCommand().startsWith("!")) {
            return parseAndExecute(request);
        }
        return null;
    }

    @SneakyThrows
    private String parseAndExecute(EngineRequest request) {
        log.debug("Handle request: {}", request);

        CommandArgs args = new CommandArgs(request.getMessage());
        AbstractCmd abstractCmd = (AbstractCmd) getCommandHandler(args.getCommand());
        if (abstractCmd != null) {

            if (args.hasArgs() && args.getArg(0).equals("?")) {
                return abstractCmd.getCommandName() + " :: usage: !?";
            }

            abstractCmd.abstractInitCommandOptions();

            String reply = abstractCmd.executeCommand(request);
            if (reply != null) {
                sendReplyMessage(request, reply);
                return reply;
            }
        }

        return null;
    }

    public void sendReplyMessage(EngineRequest request, String reply) {
        Message message
                = Message.builder()
                .sender("BotName")
                .timestamp(System.currentTimeMillis())
                .requestTimestamp(request.getTimestamp())
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
    private HokanCmd getCommandHandler(String name) {
        try {
            HokanCmd handler = this.commandHandlerLoader.getMatchingCommandHandlers(this, name);
            return handler;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
