package org.freakz.engine.commands;

import com.martiansoftware.jsap.IDMap;
import com.martiansoftware.jsap.JSAPResult;
import feign.Response;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.freakz.clients.MessageSendClient;
import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.users.User;
import org.freakz.config.ConfigService;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.api.HokanCmd;
import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.commands.util.UserAndReply;
import org.freakz.services.HokanServices;
import org.freakz.services.conversations.ConversationsService;
import org.freakz.services.wholelinetricker.WholeLineTriggers;
import org.freakz.services.wholelinetricker.WholeLineTriggersImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Iterator;

@Service
@Slf4j
public class CommandHandler {

    private final AccessService accessService;
    private final MessageSendClient messageSendClient;
    @Getter
    private final CommandHandlerLoader commandHandlerLoader;
    @Getter
    private final HokanServices hokanServices;
    private final ConfigService configService;

    private final ConversationsService conversationsService;

    private String botName = "HokanTheBot";

    public CommandHandler(AccessService accessService, MessageSendClient messageSendClient, HokanServices hokanServices, ConfigService configService, ConversationsService conversationsService) throws InitializeFailedException, IOException {
        this.accessService = accessService;
        this.messageSendClient = messageSendClient;
        this.hokanServices = hokanServices;
        this.configService = configService;
        this.conversationsService = conversationsService;
        this.commandHandlerLoader = new CommandHandlerLoader();
        if (configService != null) {
            this.botName = configService.readBotConfig().getBotConfig().getBotName();
        }
    }

    private WholeLineTriggers wholeLineTriggers = new WholeLineTriggersImpl(this);

    //    @Async
    public UserAndReply handleEngineRequest(EngineRequest request, boolean doWholeLineTriggerCheck) {

        User user = accessService.getUser(request);
        log.debug("User: {}", user);


        if (doWholeLineTriggerCheck) {
            handleWholeLineTriggers(request);
        }

        this.conversationsService.handleConversations(this, request);
        String replyMessage = null;
        if (request.getCommand().startsWith("!")) {
            replyMessage = parseAndExecute(request, user);
        }
        return UserAndReply.builder().user(user).replyMessage(replyMessage).build();


    }

    @Async
    public void handleWholeLineTriggers(EngineRequest request) {
        wholeLineTriggers.checkWholeLineTrigger(request);
    }


    @SneakyThrows
    private String parseAndExecute(EngineRequest request, User user) {
        log.debug("Handle request: {}", request);

        String message = request.getMessage();
        CommandArgs args = new CommandArgs(message);

        HandlerAlias handlerAlias = getCommandHandlerLoader().getHandlerAliasMap().get(message);
        if (handlerAlias != null) {
            log.debug("Using alias: {} = {}", handlerAlias.getAlias(), handlerAlias.getTarget());
            message = handlerAlias.getTarget();
            args = new CommandArgs(message);
        } else {
            handlerAlias = getCommandHandlerLoader().getHandlerAliasMap().get(args.getCommand());
            if (handlerAlias != null) {
                args.setCommand(handlerAlias.getTarget());

            }
        }

        AbstractCmd abstractCmd = (AbstractCmd) getCommandHandler(args.getCommand());
        if (abstractCmd != null) {
            if (abstractCmd.isAdminCommand() && !user.isAdmin()) {
                log.debug("User is not admin but command is, access denied!");
                return null;
            }


            abstractCmd.abstractInitCommandOptions();

            if (args.hasArgs() && args.getArg(0).equals("?")) {
                StringBuilder sb = new StringBuilder();
                String usage = "!" + abstractCmd.getCommandName() + " " + abstractCmd.getJsap().getUsage();
                String help = abstractCmd.getJsap().getHelp();
                sb.append("Usage    : ");
                sb.append(usage);
                sb.append("\n");

                sb.append("Help     : ");
                sb.append(help);
                sb.append("\n");

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
                request.setFromAdmin(user.isAdmin());
                String reply = abstractCmd.executeCommand(request, results);
                if (reply != null) {
                    if (request.getNetwork().equals("BOT_CLI_CLIENT")) {
                        //log.debug("Not doing sendReplyMessage() because: {}", request.getNetwork());
                    } else {
                        sendReplyMessage(request, reply);
                    }
                    return reply;
                }
            }
        }

        return null;
    }

    public void sendReplyMessage(EngineRequest request, String reply) {
        Message message
                = Message.builder()
                .sender(this.botName)
                .timestamp(System.currentTimeMillis())
                .requestTimestamp(request.getTimestamp())
                .message(reply)
                .target(request.getReplyTo())
                .id("" + request.getFromChannelId())
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
