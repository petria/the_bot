package org.freakz.engine.commands;

import com.martiansoftware.jsap.IDMap;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.common.model.users.User;
import org.freakz.common.users.UserPermissions;
import org.freakz.common.spring.rest.RestMessageSendClient;
import org.freakz.engine.commands.ai.AiCommandRegistryService;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.api.HokanCmd;
import org.freakz.engine.commands.output.ReplyOutputService;
import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.commands.util.UserAndReply;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.HokanServices;
import org.freakz.engine.services.ai.commands.HermesAiCommandService;
import org.freakz.engine.services.notifications.PrivateChatAlertService;
import org.freakz.engine.services.urls.UrlMetadataService;
import org.freakz.engine.services.wholelinetricker.WholeLineTriggers;
import org.freakz.engine.services.wholelinetricker.WholeLineTriggersImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

@Service
public class BotEngine {

  private static final Logger log = LoggerFactory.getLogger(BotEngine.class);

  private final AccessService accessService;

  private final CommandHandlerLoader commandHandlerLoader;
  private final HokanServices hokanServices;
  private final ConfigService configService;
  private final UrlMetadataService urlMetadataService;
  private final WholeLineTriggers wholeLineTriggers;
  private final RestMessageSendClient restMessageSendClient;
  private final PrivateChatAlertService privateChatAlertService;
  private final ReplyOutputService replyOutputService;
  private final CommandInvocationStatsService commandInvocationStatsService;
  private final AiCommandRegistryService aiCommandRegistryService;
  private final HermesAiCommandService hermesAiCommandService;
  private String botName = "HokanTheBot";

  public BotEngine(
      AccessService accessService,
      HokanServices hokanServices,
      ConfigService configService,
      UrlMetadataService urlMetadataService,
      RestMessageSendClient restMessageSendClient,
      PrivateChatAlertService privateChatAlertService,
      ReplyOutputService replyOutputService,
      CommandInvocationStatsService commandInvocationStatsService,
      AiCommandRegistryService aiCommandRegistryService,
      HermesAiCommandService hermesAiCommandService)
      throws InitializeFailedException, IOException {
    this.accessService = accessService;
    this.hokanServices = hokanServices;
    this.configService = configService;
//    this.countInterceptor = countInterceptor;
    this.urlMetadataService = urlMetadataService;
    this.restMessageSendClient = restMessageSendClient;
    this.privateChatAlertService = privateChatAlertService;
    this.replyOutputService = replyOutputService;
    this.commandInvocationStatsService = commandInvocationStatsService;
    this.aiCommandRegistryService = aiCommandRegistryService;
    this.hermesAiCommandService = hermesAiCommandService;

    if (configService != null) {
      this.botName = configService.readBotConfig().getBotConfig().getBotName();
    }
    this.commandHandlerLoader =
        new CommandHandlerLoader(configService.getActiveProfile(), this.botName);
    this.wholeLineTriggers = new WholeLineTriggersImpl(this);
  }

  public CommandHandlerLoader getCommandHandlerLoader() {
    return commandHandlerLoader;
  }

  public HokanServices getHokanServices() {
    return hokanServices;
  }

  public ConfigService getConfigService() {
    return configService;
  }

  public UsersService getUsersService() {
    return accessService.getUsersService();
  }

  public ReplyOutputService getReplyOutputService() {
    return replyOutputService;
  }

  public AiCommandRegistryService getAiCommandRegistryService() {
    return aiCommandRegistryService;
  }

  public UserAndReply handleEngineRequest(EngineRequest request, boolean doWholeLineTriggerCheck) throws Exception {

    request.setBotConfig(configService.readBotConfig());

    User user = accessService.getUser(request);
    log.debug("User: {}", user);
    privateChatAlertService.notifyUnknownPrivateChatIfNeeded(request, user);

    String originalCommand = request.getCommand();
    boolean explicitCommand =
        originalCommand.startsWith("!") || originalCommand.startsWith(this.botName);
    boolean implicitPrivateOpenClawChat =
        request.isPrivateChannel() && !explicitCommand;
    boolean implicitPublicOpenClawChat =
        !request.isPrivateChannel() && !explicitCommand && shouldHandlePublicAiChat(request);
    boolean implicitOpenClawChat = implicitPrivateOpenClawChat || implicitPublicOpenClawChat;

    String wholeLine = null;
    if (doWholeLineTriggerCheck) {
      wholeLine = handleWholeLineTriggers(request);
    }

    if (!request.getCommand().startsWith(this.botName) && !implicitOpenClawChat) {
      this.urlMetadataService.handleEngineRequest(request, this);
    }

    String replyMessage = null;
    if (implicitOpenClawChat) {
      request.setCommand("!hokan " + originalCommand);
      replyMessage = parseAndExecute(request, user, false);
    } else if (explicitCommand) {
      replyMessage = parseAndExecute(request, user, true);
    }
    if (wholeLine != null) {
      replyMessage += " WL: " + wholeLine;
    }
    return UserAndReply.builder().user(user).replyMessage(replyMessage).build();
  }

  public String handleWholeLineTriggers(EngineRequest request) {
    return wholeLineTriggers.checkWholeLineTrigger(request);
  }

  private boolean shouldHandlePublicAiChat(EngineRequest request) {
    String echoToAlias = request.getEchoToAlias();
    if (echoToAlias == null || echoToAlias.isBlank()) {
      return false;
    }

    TheBotConfig botConfig = request.getBotConfig();
    Channel configuredChannel = findChannelByEchoToAlias(botConfig, echoToAlias);
    if (configuredChannel == null || !Boolean.TRUE.equals(configuredChannel.getPublicAiEnabled())) {
      return false;
    }

    String message = request.getMessage();
    if (message == null || message.isBlank()) {
      return false;
    }

    return message.contains("?") || containsBotMention(message);
  }

  private Channel findChannelByEchoToAlias(TheBotConfig botConfig, String echoToAlias) {
    if (botConfig == null || echoToAlias == null || echoToAlias.isBlank()) {
      return null;
    }
    for (IrcServerConfig ircConfig : nullSafe(botConfig.getIrcServerConfigs())) {
      Channel channel = findChannelInList(ircConfig == null ? null : ircConfig.getChannelList(), echoToAlias);
      if (channel != null) {
        return channel;
      }
    }
    Channel discordChannel = findChannelInList(
        botConfig.getDiscordConfig() == null ? null : botConfig.getDiscordConfig().getChannelList(),
        echoToAlias);
    if (discordChannel != null) {
      return discordChannel;
    }
    Channel telegramChannel = findChannelInList(
        botConfig.getTelegramConfig() == null ? null : botConfig.getTelegramConfig().getChannelList(),
        echoToAlias);
    if (telegramChannel != null) {
      return telegramChannel;
    }
    return findChannelInList(
        botConfig.getWhatsappConfig() == null ? null : botConfig.getWhatsappConfig().getChannelList(),
        echoToAlias);
  }

  private Channel findChannelInList(List<Channel> channels, String echoToAlias) {
    for (Channel channel : nullSafe(channels)) {
      if (channel != null && channel.getEchoToAlias() != null && channel.getEchoToAlias().equalsIgnoreCase(echoToAlias)) {
        return channel;
      }
    }
    return null;
  }

  private <T> List<T> nullSafe(List<T> values) {
    return values == null ? List.of() : values;
  }

  private boolean containsBotMention(String message) {
    if (message == null || message.isBlank() || botName == null || botName.isBlank()) {
      return false;
    }

    String messageLower = message.toLowerCase();
    String botNameLower = botName.toLowerCase();

    return messageLower.contains(botNameLower)
        || messageLower.contains(botNameLower + ":")
        || messageLower.contains("@" + botNameLower);
  }

  private String parseAndExecute(EngineRequest request, User user, boolean sendProcessingIndicator) throws Exception {
    log.debug("Handle request: {}", request.getCommand());

    String message = request.getMessage();
    CommandArgs args = new CommandArgs(message);

    if (executeAiCommandIfMatched(request, user, args, sendProcessingIndicator)) {
      return null;
    }

    CommandHandlerLoader.AliasResolution aliasResolution = getCommandHandlerLoader().resolveAlias(message);
    if (aliasResolution.isError()) {
      return aliasResolution.errorMessage();
    }
    if (aliasResolution.isAliased()) {
      HandlerAlias handlerAlias = aliasResolution.alias();
      log.debug("Using alias: {} = {}", handlerAlias.getAlias(), handlerAlias.getTarget());
      message = aliasResolution.resolvedMessage();
      args = new CommandArgs(message);
    }

    if (executeAiCommandIfMatched(request, user, args, sendProcessingIndicator)) {
      return null;
    }

    HandlerClass handlerClass = this.commandHandlerLoader.getHandlerClassForCommand(args.getCommand());
    AbstractCmd abstractCmd = (AbstractCmd) getCommandHandler(args.getCommand());
    if (abstractCmd != null) {
      commandInvocationStatsService.recordInvocation(handlerClass);
      request.setUser(user);

      String requiredPermission = abstractCmd.getRequiredPermission();
      if (requiredPermission != null && !requiredPermission.isBlank()
          && !UserPermissions.has(user, requiredPermission)
          && !isAnonymousAiCommandAllowed(abstractCmd, request, user)) {
        log.debug("User lacks required command permission: {}", requiredPermission);
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
        reply =
            String.format(
                "Invalid arguments, usage: %s %s",
                abstractCmd.getCommandName(), abstractCmd.getJsap().getUsage());
      } else {
        try {
          if (sendProcessingIndicator) {
            sendProcessingIndicator(request);
          }
          reply = abstractCmd.executeCommand(request, results);

        } catch (Exception e) {
          reply = "Command failed: " + e.getMessage();
        }
      }

      if (reply != null) {
        return sendReplyMessage(request, reply);
      }
    }
    return null;
  }

  private boolean executeAiCommandIfMatched(
      EngineRequest request,
      User user,
      CommandArgs args,
      boolean sendProcessingIndicator) {
    return aiCommandRegistryService.resolve(args.getCommand())
        .map(command -> {
          commandInvocationStatsService.recordDynamicInvocation(
              AiCommandRegistryService.PROVIDER_NAMESPACE,
              command.getName());
          request.setUser(user);
          if (!hasAiCommandPermission(user, command)) {
            log.debug("User lacks required AI command permission: {}", command.getRequiredPermission());
            return true;
          }
          if (sendProcessingIndicator) {
            sendProcessingIndicator(request);
          }
          hermesAiCommandService.ask(request, command, args.joinArgs(0));
          return true;
        })
        .orElse(false);
  }

  private boolean hasAiCommandPermission(User user, AiCommandDefinition command) {
    String requiredPermission = command.getRequiredPermission();
    return requiredPermission == null
        || requiredPermission.isBlank()
        || UserPermissions.has(user, requiredPermission);
  }

  private void sendProcessingIndicator(EngineRequest request) {
    if (request.getFromConnectionId() < 0) {
      return;
    }
    Message message =
        Message.builder()
            .sender(this.botName)
            .timestamp(System.currentTimeMillis())
            .time(LocalDateTime.now())
            .requestTimestamp(request.getTimestamp())
            .message("processing")
            .messageSource(MessageSource.NONE)
            .target(request.getReplyTo())
            .id(request.getFromChannelId() == null ? null : "" + request.getFromChannelId())
            .build();
    try {
      ResponseEntity<String> response =
          restMessageSendClient.sendProcessingIndicator(request.getFromConnectionId(), message);
      log.debug("Processing indicator status: {}", response.getStatusCode());
    } catch (Exception ex) {
      log.debug("Processing indicator failed: {}", ex.getMessage());
    }
  }

  private boolean isAnonymousAiCommandAllowed(AbstractCmd abstractCmd, EngineRequest request, User user) {
    if (!"hokan".equalsIgnoreCase(abstractCmd.getCommandName())) {
      return false;
    }
    if (request == null || request.isPrivateChannel() || !isUnknownUser(user)) {
      return false;
    }

    Channel configuredChannel = findChannelByEchoToAlias(request.getBotConfig(), request.getEchoToAlias());
    return configuredChannel != null
        && Boolean.TRUE.equals(configuredChannel.getPublicAiEnabled())
        && Boolean.TRUE.equals(configuredChannel.getAllowAnonymousAiCommands());
  }

  private boolean isUnknownUser(User user) {
    User unknownUser = accessService.getUsersService().getNotKnownUser();
    return user != null
        && user.getId() != null
        && unknownUser != null
        && user.getId().equals(unknownUser.getId());
  }

  public String sendReplyMessage(EngineRequest request, String reply) {

    if ("BOT_CLI_CLIENT".equals(request.getNetwork()) || "BOT_INTERNAL".equals(request.getNetwork())) {
      // log.debug("Not doing sendReplyMessage() because: {}", request.getNetwork());
// TODO       countInterceptor.computeCount("OUT: commandHandler");
      return reply;
    } else {
      String formattedReply = replyOutputService.formatReply(request, reply);
      Message message =
          Message.builder()
              .sender(this.botName)
              .timestamp(System.currentTimeMillis())
              .time(LocalDateTime.now())
              .requestTimestamp(request.getTimestamp())
              .message(formattedReply)
              .messageSource(MessageSource.NONE)
              .target(request.getReplyTo())
              .id("" + request.getFromChannelId())
              .build();
      try {
//        Response response = messageSendClient.sendMessage(request.getFromConnectionId(), message);
        ResponseEntity<String> response = restMessageSendClient.sendMessage(request.getFromConnectionId(), message);
        HttpStatusCode statusCode = response.getStatusCode();
        log.debug("Reply status: {}", statusCode);

/*        int status = response.status();
        log.debug("reply send status: {}", status);
        if (status != 200) {
          String bodyJson =
              new BufferedReader(new InputStreamReader(response.body().asInputStream()))
                  .lines()
                  .parallel()
                  .collect(Collectors.joining("\n"));

          log.debug("bodyJson: {}", bodyJson);
        }
*/
      } catch (Exception ex) {
        log.error("Sending reply failed: {}", ex.getMessage());
      }
      return formattedReply;
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
