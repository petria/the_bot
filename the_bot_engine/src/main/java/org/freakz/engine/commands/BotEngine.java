package org.freakz.engine.commands;

import com.martiansoftware.jsap.IDMap;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.InitializeFailedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.feed.Message;
import org.freakz.common.model.feed.MessageSource;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.common.model.users.User;
import org.freakz.common.users.UserPermissions;
import org.freakz.common.spring.rest.RestMessageSendClient;
import org.freakz.engine.commands.ai.AiCommandHelpFormatter;
import org.freakz.engine.commands.ai.AiCommandRegistryService;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.api.HokanCmd;
import org.freakz.engine.commands.output.ReplyOutputService;
import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.commands.util.UserAndReply;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.config.ConfiguredChannelResolver;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.HokanServices;
import org.freakz.engine.services.ProcessingIndicatorService;
import org.freakz.engine.services.ai.commands.HermesAiCommandService;
import org.freakz.engine.services.console.ConsoleOutputService;
import org.freakz.engine.services.notifications.PrivateChatAlertService;
import org.freakz.engine.services.urls.UrlExtractor;
import org.freakz.engine.services.urls.UrlResolutionService;
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
import java.util.Locale;

@Service
public class BotEngine {

  private static final Logger log = LoggerFactory.getLogger(BotEngine.class);

  private final AccessService accessService;

  private final CommandHandlerLoader commandHandlerLoader;
  private final CommandProviderRegistry commandProviderRegistry;
  private final HokanServices hokanServices;
  private final ConfigService configService;
  private final UrlResolutionService urlResolutionService;
  private final WholeLineTriggers wholeLineTriggers;
  private final RestMessageSendClient restMessageSendClient;
  private final PrivateChatAlertService privateChatAlertService;
  private final ReplyOutputService replyOutputService;
  private final CommandInvocationStatsService commandInvocationStatsService;
  private final AiCommandRegistryService aiCommandRegistryService;
  private final HermesAiCommandService hermesAiCommandService;
  private final ConfiguredChannelResolver configuredChannelResolver;
  private final ConsoleOutputService consoleOutputService;
  private final ProcessingIndicatorService processingIndicatorService;
  private String botName = "HokanTheBot";

  public BotEngine(
      AccessService accessService,
      HokanServices hokanServices,
      ConfigService configService,
      UrlResolutionService urlResolutionService,
      RestMessageSendClient restMessageSendClient,
      PrivateChatAlertService privateChatAlertService,
      ReplyOutputService replyOutputService,
      CommandInvocationStatsService commandInvocationStatsService,
      AiCommandRegistryService aiCommandRegistryService,
      HermesAiCommandService hermesAiCommandService,
      ConfiguredChannelResolver configuredChannelResolver,
      ConsoleOutputService consoleOutputService,
      ProcessingIndicatorService processingIndicatorService)
      throws InitializeFailedException, IOException {
    this.accessService = accessService;
    this.hokanServices = hokanServices;
    this.configService = configService;
//    this.countInterceptor = countInterceptor;
    this.urlResolutionService = urlResolutionService;
    this.restMessageSendClient = restMessageSendClient;
    this.privateChatAlertService = privateChatAlertService;
    this.replyOutputService = replyOutputService;
    this.commandInvocationStatsService = commandInvocationStatsService;
    this.aiCommandRegistryService = aiCommandRegistryService;
    this.hermesAiCommandService = hermesAiCommandService;
    this.configuredChannelResolver = configuredChannelResolver;
    this.consoleOutputService = consoleOutputService;
    this.processingIndicatorService = processingIndicatorService;

    if (configService != null) {
      this.botName = configService.readBotConfig().getBotConfig().getBotName();
    }
    this.commandHandlerLoader =
        new CommandHandlerLoader(configService.getActiveProfile(), this.botName);
    this.commandProviderRegistry = new CommandProviderRegistry(this.commandHandlerLoader, this.aiCommandRegistryService);
    this.wholeLineTriggers = new WholeLineTriggersImpl(this);
  }

  public CommandHandlerLoader getCommandHandlerLoader() {
    return commandHandlerLoader;
  }

  public CommandProviderRegistry getCommandProviderRegistry() {
    return commandProviderRegistry;
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
      this.urlResolutionService.handleEngineRequest(request, this);
    }

    String replyMessage = null;
    if (implicitOpenClawChat) {
      request.setCommand("!hokan " + originalCommand);
      // Implicit public/private AI chats are asynchronous just like explicit !hokan
      // commands, so keep the processing indicator active until the reply arrives.
      replyMessage = parseAndExecute(request, user, true);
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
    Channel configuredChannel = configuredChannelResolver.findByEchoToAlias(botConfig, echoToAlias);
    if (configuredChannel == null || !Boolean.TRUE.equals(configuredChannel.getPublicAiEnabled())) {
      return false;
    }

    String message = request.getMessage();
    if (message == null || message.isBlank()) {
      return false;
    }

    String messageWithoutUrls = UrlExtractor.removeUrls(message);
    if (request.isBotMentioned() || containsBotMention(messageWithoutUrls)) {
      return true;
    }
    return messageWithoutUrls.contains("?") && !isAddressedToAnotherUser(messageWithoutUrls);
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

  private boolean isAddressedToAnotherUser(String message) {
    if (message == null || message.isBlank()) {
      return false;
    }
    String trimmed = message.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    String botNameLower = botName == null ? "" : botName.toLowerCase(Locale.ROOT);

    if (trimmed.startsWith("@") || trimmed.startsWith("<@")) {
      return botNameLower.isBlank() || !lower.startsWith("@" + botNameLower);
    }

    int separator = Math.min(nonNegativeIndexOf(trimmed, ':'), nonNegativeIndexOf(trimmed, ','));
    if (separator <= 0 || separator > 32) {
      return false;
    }

    String addressedName = trimmed.substring(0, separator).trim();
    if (addressedName.isBlank() || addressedName.contains(" ")) {
      return false;
    }
    return !addressedName.equalsIgnoreCase(botName);
  }

  private int nonNegativeIndexOf(String value, char ch) {
    int idx = value.indexOf(ch);
    return idx < 0 ? Integer.MAX_VALUE : idx;
  }

  private String parseAndExecute(EngineRequest request, User user, boolean sendProcessingIndicator) throws Exception {
    log.debug("Handle request: {}", request.getCommand());

    String message = request.getMessage();
    CommandArgs args = new CommandArgs(message);

    CommandProviderRegistry.ResolvedCommand resolvedCommand = commandProviderRegistry.resolve(args.getCommand()).orElse(null);
    if (resolvedCommand == null) {
      CommandHandlerLoader.AliasResolution aliasResolution = commandProviderRegistry.resolveAlias(message);
      if (aliasResolution.isError()) {
        return aliasResolution.errorMessage();
      }
      if (aliasResolution.isAliased()) {
        HandlerAlias handlerAlias = aliasResolution.alias();
        log.debug("Using alias: {} = {}", handlerAlias.getAlias(), handlerAlias.getTarget());
        message = aliasResolution.resolvedMessage();
        args = new CommandArgs(message);
        resolvedCommand = commandProviderRegistry.resolve(args.getCommand()).orElse(null);
      }
    }

    if (resolvedCommand == null) {
      return null;
    }
    if (resolvedCommand.command().isAiCommand()) {
      return executeAiCommand(request, user, args, resolvedCommand.command(), sendProcessingIndicator).reply();
    }

    HandlerClass handlerClass = resolvedCommand.command().handlerClass();
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
            startProcessingIndicator(request);
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

  private AiCommandExecutionResult executeAiCommand(
      EngineRequest request,
      User user,
      CommandArgs args,
      CommandProviderRegistry.CommandRegistration commandRegistration,
      boolean sendProcessingIndicator) {
    AiCommandDefinition command = commandRegistration.aiCommand();
    commandInvocationStatsService.recordDynamicInvocation(commandRegistration.namespace(), command.getName());
    request.setUser(user);
    if (!hasAiCommandPermission(user, command)) {
      log.debug("User lacks required AI command permission: {}", command.getRequiredPermission());
      return AiCommandExecutionResult.matched(null);
    }
    if (args.hasArgs() && "?".equals(args.getArg(0))) {
      return AiCommandExecutionResult.matched(
          sendReplyMessage(request, AiCommandHelpFormatter.formatDetailed(command)));
    }
    if (sendProcessingIndicator) {
      startProcessingIndicator(request);
    }
    hermesAiCommandService.ask(request, command, args.joinArgs(0));
    return AiCommandExecutionResult.matched(null);
  }

  private boolean hasAiCommandPermission(User user, AiCommandDefinition command) {
    String requiredPermission = command.getRequiredPermission();
    return requiredPermission == null
        || requiredPermission.isBlank()
        || UserPermissions.has(user, requiredPermission);
  }

  private void startProcessingIndicator(EngineRequest request) {
    processingIndicatorService.start(request, this.botName);
  }

  private boolean isAnonymousAiCommandAllowed(AbstractCmd abstractCmd, EngineRequest request, User user) {
    if (!"hokan".equalsIgnoreCase(abstractCmd.getCommandName())) {
      return false;
    }
    if (request == null || request.isPrivateChannel() || !isUnknownUser(user)) {
      return false;
    }

    Channel configuredChannel = configuredChannelResolver.findByEchoToAlias(request.getBotConfig(), request.getEchoToAlias());
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

    try {
      return sendReplyMessageInternal(request, reply);
    } finally {
      processingIndicatorService.stop(request);
    }
  }

  private String sendReplyMessageInternal(EngineRequest request, String reply) {

    if (ConsoleOutputService.NETWORK.equals(request.getNetwork())) {
      consoleOutputService.recordReply(request, reply);
      return reply;
    }

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
              .replyToMessageId(request.getReplyToMessageId())
              .messageThreadId(request.getMessageThreadId())
              .replyToSenderId(request.getReplyToSenderId())
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

  private record AiCommandExecutionResult(boolean matched, String reply) {
    private static AiCommandExecutionResult matched(String reply) {
      return new AiCommandExecutionResult(true, reply);
    }

    private static AiCommandExecutionResult notMatched() {
      return new AiCommandExecutionResult(false, null);
    }
  }
}
