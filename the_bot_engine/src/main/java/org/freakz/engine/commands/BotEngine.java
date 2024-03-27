package org.freakz.engine.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.martiansoftware.jsap.IDMap;
import com.martiansoftware.jsap.JSAPResult;
import feign.Response;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.users.User;
import org.freakz.common.util.FeignUtils;
import org.freakz.engine.clients.MessageSendClient;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.api.HokanCmd;
import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.commands.util.UserAndReply;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.HokanServices;
import org.freakz.engine.services.conversations.ConversationsService;
import org.freakz.engine.services.status.CallCountInterceptor;
import org.freakz.engine.services.urls.UrlMetadataService;
import org.freakz.engine.services.wholelinetricker.WholeLineTriggers;
import org.freakz.engine.services.wholelinetricker.WholeLineTriggersImpl;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

@Service
@Slf4j
public class BotEngine {

    private final AccessService accessService;
    private final MessageSendClient messageSendClient;
    @Getter
    private final CommandHandlerLoader commandHandlerLoader;
    @Getter
    private final HokanServices hokanServices;
    private final ConfigService configService;

    private final ConversationsService conversationsService;

    private final CallCountInterceptor countInterceptor;

    private final UrlMetadataService urlMetadataService;

    private String botName = "HokanTheBot";

    public BotEngine(AccessService accessService, MessageSendClient messageSendClient, HokanServices hokanServices, ConfigService configService, ConversationsService conversationsService, CallCountInterceptor countInterceptor, UrlMetadataService urlMetadataService) throws InitializeFailedException, IOException {
        this.accessService = accessService;
        this.messageSendClient = messageSendClient;
        this.hokanServices = hokanServices;
        this.configService = configService;
        this.conversationsService = conversationsService;
        this.countInterceptor = countInterceptor;
        this.urlMetadataService = urlMetadataService;
        if (configService != null) {
            this.botName = configService.readBotConfig().getBotConfig().getBotName();
        }
        this.commandHandlerLoader = new CommandHandlerLoader(configService.getActiveProfile());
    }

    private WholeLineTriggers wholeLineTriggers = new WholeLineTriggersImpl(this);

    @SneakyThrows
    public UserAndReply handleEngineRequest(EngineRequest request, boolean doWholeLineTriggerCheck) {

        request.setBotConfig(configService.readBotConfig());

        User user = accessService.getUser(request);
        log.debug("User: {}", user);

        String wholeLine = null;
        if (doWholeLineTriggerCheck) {
            wholeLine = handleWholeLineTriggers(request);
        }

        this.urlMetadataService.handleEngineRequest(request, this);
        this.conversationsService.handleConversations(this, request);

        String replyMessage = null;
        if (request.getCommand().startsWith("!")) {
            replyMessage = parseAndExecute(request, user);
        }
        if (wholeLine != null) {
            replyMessage += " WL: " + wholeLine;
        }
        return UserAndReply.builder().user(user).replyMessage(replyMessage).build();


    }


    public String handleWholeLineTriggers(EngineRequest request) {
        return wholeLineTriggers.checkWholeLineTrigger(request);
    }


    @SneakyThrows
    private String parseAndExecute(EngineRequest request, User user) {
        log.debug("Handle request: {}", request.getCommand());

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
            Iterator<?> iterator = map.idIterator();

            String argsLine = args.joinArgs(0);
            if (iterator.hasNext()) {
                results = abstractCmd.getJsap().parse(argsLine);
                parseRes = results.success();
            } else {
                parseRes = true;
            }

            String reply;
            if (!parseRes) {
                reply = String.format("Invalid arguments, usage: %s %s", abstractCmd.getCommandName(), abstractCmd.getJsap().getUsage());
            } else {
                request.setFromAdmin(user.isAdmin());
                request.setUser(user);
                reply = abstractCmd.executeCommand(request, results);
            }

            if (reply != null) {
                return sendReplyMessage(request, reply);
            }
        }
        return null;
    }

    public String sendReplyMessage(EngineRequest request, String reply) {

        if (request.getNetwork().equals("BOT_CLI_CLIENT")) {
            //log.debug("Not doing sendReplyMessage() because: {}", request.getNetwork());
            countInterceptor.computeCount("OUT: commandHandler");
            return null;
        } else {
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
                log.debug("Response: {}", response);
                Optional<String> responseBody = FeignUtils.getResponseBody(response, String.class, new ObjectMapper());
                responseBody.ifPresent(s -> log.debug("responseBody: {}", s));

            } catch (Exception ex) {
                log.error("Sending reply failed: {}", ex.getMessage());
            }
            return reply;

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
