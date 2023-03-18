package org.freakz.engine.commands;

import com.martiansoftware.jsap.IDMap;
import com.martiansoftware.jsap.JSAPResult;
import feign.Response;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Iterator;

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

    @Async
    public String handleEngineRequest(EngineRequest request) {
        log.debug("Start handle in service >>>");
        return handleEngineRequest(request, false);
    }

    @Async
    public String handleEngineRequest(EngineRequest request, boolean doWholeLineTriggerCheck) {

        if (doWholeLineTriggerCheck) {
            handleWholeLineTriggers(request);
        }

        if (request.getCommand().startsWith("!")) {
            return parseAndExecute(request);
        }
        return null;
    }

    @Async
    public void handleWholeLineTriggers(EngineRequest request) {
        wholeLineTriggers.checkWholeLineTrigger(request);
    }


    @SneakyThrows
    private String parseAndExecute(EngineRequest request) {
        log.debug("Handle request: {}", request);

        CommandArgs args = new CommandArgs(request.getMessage());
        AbstractCmd abstractCmd = (AbstractCmd) getCommandHandler(args.getCommand());
        if (abstractCmd != null) {

            abstractCmd.abstractInitCommandOptions();

            if (args.hasArgs() && args.getArg(0).equals("?")) {
                StringBuilder sb = new StringBuilder();
                String usage = "!" + abstractCmd.getCommandName() + " " + abstractCmd.getJsap().getUsage();
                String help = abstractCmd.getJsap().getHelp();
                sb.append("Usage    : ");
                sb.append(usage);
//                sb.append("\n");

/*                sb.append("Help     : ");
                sb.append(help);
                sb.append("|");*/

                sendReplyMessage(request, sb.toString());
                return sb.toString();
            }

            boolean parseRes;
            JSAPResult results = null;
            IDMap map = abstractCmd.getJsap().getIDMap();
            Iterator iterator = map.idIterator();

            String argsLine = args.joinArgs(0);
            if (iterator.hasNext()) {
                results = abstractCmd.getJsap().parse(argsLine);
                parseRes = results.success();
            } else {
                parseRes = true;
            }

            if (!parseRes) {
                String reply = String.format("Invalid arguments, usage: %s %s", abstractCmd.getCommandName(), abstractCmd.getJsap().getUsage());
                sendReplyMessage(request, reply);
                return reply;

            } else {
                String reply = abstractCmd.executeCommand(request, results);
                if (reply != null) {
                    sendReplyMessage(request, reply);
                    return reply;
                }
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
                .id(""+ request.getFromChannelId())
                .build();

        try {
            Response response = messageSendClient.sendMessage(request.getFromConnectionId(), message);
            int status = response.status();
            log.debug("reply send status: {}", status);
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
