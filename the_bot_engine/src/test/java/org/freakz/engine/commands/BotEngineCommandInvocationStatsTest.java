package org.freakz.engine.commands;

import org.freakz.common.model.botconfig.BotConfig;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.DiscordConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.aicommand.AiCommandDefinition;
import org.freakz.common.model.users.User;
import org.freakz.engine.commands.ai.AiCommandRegistryService;
import org.freakz.engine.commands.output.ReplyOutputService;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.config.ConfiguredChannelResolver;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.HokanServices;
import org.freakz.engine.services.ai.commands.HermesAiCommandService;
import org.freakz.engine.services.console.ConsoleOutputService;
import org.freakz.engine.services.notifications.PrivateChatAlertService;
import org.freakz.engine.services.urls.UrlResolutionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BotEngineCommandInvocationStatsTest {

  @Test
  void countsMatchedPublicCommands() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    BotEngine botEngine = botEngine(statsService);

    botEngine.handleEngineRequest(request("!ping"), true);

    assertThat(statsService.getCommandInvocationCount("main::ping")).isEqualTo(1);
    assertThat(statsService.getProviderInvocationCount("main")).isEqualTo(1);
  }

  @Test
  void countsPermissionDeniedBotInfoAliasAsBotInfo() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    BotEngine botEngine = botEngine(statsService);

    botEngine.handleEngineRequest(request("!buildinfo"), true);

    assertThat(statsService.getCommandInvocationCount("main::botinfo")).isEqualTo(1);
    assertThat(statsService.getCommandInvocationCount("main::buildinfo")).isZero();
    assertThat(statsService.getProviderInvocationCount("main")).isEqualTo(1);
  }

  @Test
  void doesNotCountUnknownCommands() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    BotEngine botEngine = botEngine(statsService);

    botEngine.handleEngineRequest(request("!doesnotexist"), true);

    assertThat(statsService.getProviderInvocationCount("main")).isZero();
  }

  @Test
  void youtubeQueryStringDoesNotTriggerImplicitPublicAiChat() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    UrlResolutionService urlResolutionService = mock(UrlResolutionService.class);
    BotEngine botEngine = botEngine(
        statsService,
        emptyAiRegistry(),
        mock(HermesAiCommandService.class),
        publicAiConfig(),
        urlResolutionService);

    botEngine.handleEngineRequest(publicRequest(
        "testi https://www.youtube.com/watch?v=_IZBMQS3JGE"), true);

    verify(urlResolutionService).handleEngineRequest(any(), any());
  }

  @Test
  void publicAiQuestionAddressedToMentionedUserDoesNotTriggerImplicitAiChat() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    UrlResolutionService urlResolutionService = mock(UrlResolutionService.class);
    BotEngine botEngine = botEngine(
        statsService,
        emptyAiRegistry(),
        mock(HermesAiCommandService.class),
        publicAiConfig(),
        urlResolutionService);

    String reply = botEngine.handleEngineRequest(publicRequest(
        "@42550241054949 did u try thinking per request?"), true).getReplyMessage();

    assertThat(reply).isNull();
    assertThat(statsService.getCommandInvocationCount("main::hokan")).isZero();
    verify(urlResolutionService).handleEngineRequest(any(), any());
  }

  @Test
  void publicAiQuestionAddressedToDiscordMentionDoesNotTriggerImplicitAiChat() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    UrlResolutionService urlResolutionService = mock(UrlResolutionService.class);
    BotEngine botEngine = botEngine(
        statsService,
        emptyAiRegistry(),
        mock(HermesAiCommandService.class),
        publicAiConfig(),
        urlResolutionService);

    String reply = botEngine.handleEngineRequest(publicRequest(
        "<@265828694445129728> did u try thinking per request?"), true).getReplyMessage();

    assertThat(reply).isNull();
    assertThat(statsService.getCommandInvocationCount("main::hokan")).isZero();
    verify(urlResolutionService).handleEngineRequest(any(), any());
  }

  @Test
  void publicAiQuestionAddressedToNamedUserDoesNotTriggerImplicitAiChat() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    UrlResolutionService urlResolutionService = mock(UrlResolutionService.class);
    BotEngine botEngine = botEngine(
        statsService,
        emptyAiRegistry(),
        mock(HermesAiCommandService.class),
        publicAiConfig(),
        urlResolutionService);

    String reply = botEngine.handleEngineRequest(publicRequest("Oscar: did u try thinking per request?"), true)
        .getReplyMessage();

    assertThat(reply).isNull();
    assertThat(statsService.getCommandInvocationCount("main::hokan")).isZero();
    verify(urlResolutionService).handleEngineRequest(any(), any());
  }

  @Test
  void dynamicAiQuestionReturnsUsageWithoutCallingHermes() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    AiCommandDefinition dynping = aiCommand("dynping", "!dynping <text>", "Test dynamic command.");
    AiCommandRegistryService aiCommandRegistryService = mock(AiCommandRegistryService.class);
    when(aiCommandRegistryService.resolve(anyString())).thenAnswer(invocation ->
        "dynping".equals(invocation.getArgument(0)) ? Optional.of(dynping) : Optional.empty());
    HermesAiCommandService hermesAiCommandService = mock(HermesAiCommandService.class);
    BotEngine botEngine = botEngine(statsService, aiCommandRegistryService, hermesAiCommandService);

    String reply = botEngine.handleEngineRequest(request("!dynping ?"), true).getReplyMessage();

    assertThat(reply).contains("Usage    : !dynping <text>");
    assertThat(reply).contains("Help     : Test dynamic command.");
    assertThat(statsService.getCommandInvocationCount("ai::dynping")).isEqualTo(1);
    assertThat(statsService.getProviderInvocationCount("ai")).isEqualTo(1);
    verify(hermesAiCommandService, never()).ask(any(), any(), anyString());
  }

  @Test
  void namespacedDynamicAiQuestionReturnsUsageWithoutCallingHermes() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    AiCommandDefinition dynping = aiCommand("dynping", "!dynping <text>", "Test dynamic command.");
    AiCommandRegistryService aiCommandRegistryService = mock(AiCommandRegistryService.class);
    when(aiCommandRegistryService.resolve("dynping")).thenReturn(Optional.of(dynping));
    HermesAiCommandService hermesAiCommandService = mock(HermesAiCommandService.class);
    BotEngine botEngine = botEngine(statsService, aiCommandRegistryService, hermesAiCommandService);

    String reply = botEngine.handleEngineRequest(request("!ai::dynping ?"), true).getReplyMessage();

    assertThat(reply).contains("Usage    : !dynping <text>");
    assertThat(reply).contains("Help     : Test dynamic command.");
    assertThat(statsService.getCommandInvocationCount("ai::dynping")).isEqualTo(1);
    verify(hermesAiCommandService, never()).ask(any(), any(), anyString());
  }

  @Test
  void helpShowsDynamicAiCommandDetails() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    AiCommandDefinition dynping = aiCommand("dynping", "!dynping <text>", "Test dynamic command.");
    dynping.setAliases(List.of("!pong-ai"));
    AiCommandRegistryService aiCommandRegistryService = mock(AiCommandRegistryService.class);
    when(aiCommandRegistryService.resolve(anyString())).thenReturn(Optional.empty());
    when(aiCommandRegistryService.resolveAny("dynping")).thenReturn(Optional.of(dynping));
    HermesAiCommandService hermesAiCommandService = mock(HermesAiCommandService.class);
    BotEngine botEngine = botEngine(statsService, aiCommandRegistryService, hermesAiCommandService);

    String reply = botEngine.handleEngineRequest(request("!help dynping"), true).getReplyMessage();

    assertThat(reply).contains("Usage    : !dynping <text>");
    assertThat(reply).contains("Aliases  : !pong-ai");
    assertThat(reply).contains("Help     : Test dynamic command.");
  }

  private BotEngine botEngine(CommandInvocationStatsService statsService) throws Exception {
    return botEngine(statsService, emptyAiRegistry(), mock(HermesAiCommandService.class));
  }

  private BotEngine botEngine(
      CommandInvocationStatsService statsService,
      AiCommandRegistryService aiCommandRegistryService,
      HermesAiCommandService hermesAiCommandService) throws Exception {
    return botEngine(
        statsService,
        aiCommandRegistryService,
        hermesAiCommandService,
        TheBotConfig.builder()
            .botConfig(BotConfig.builder().botName("HokanDEV").build())
            .build(),
        mock(UrlResolutionService.class));
  }

  private BotEngine botEngine(
      CommandInvocationStatsService statsService,
      AiCommandRegistryService aiCommandRegistryService,
      HermesAiCommandService hermesAiCommandService,
      TheBotConfig botConfig,
      UrlResolutionService urlResolutionService) throws Exception {
    ConfigService configService = mock(ConfigService.class);
    when(configService.getActiveProfile()).thenReturn("DEV");
    when(configService.readBotConfig()).thenReturn(botConfig);

    UsersService usersService = mock(UsersService.class);
    User unknownUser = User.builder().username("unknown").permissions(List.of()).build();
    unknownUser.setId(-1L);
    when(usersService.findAll()).thenReturn(List.of());
    when(usersService.getNotKnownUser()).thenReturn(unknownUser);

    return new BotEngine(
        new AccessService(usersService),
        mock(HokanServices.class),
        configService,
        urlResolutionService,
        null,
        mock(PrivateChatAlertService.class),
        mock(ReplyOutputService.class),
        statsService,
        aiCommandRegistryService,
        hermesAiCommandService,
        new ConfiguredChannelResolver(),
        new ConsoleOutputService());
  }

  private AiCommandRegistryService emptyAiRegistry() {
    AiCommandRegistryService aiCommandRegistryService = mock(AiCommandRegistryService.class);
    when(aiCommandRegistryService.resolve(anyString())).thenReturn(Optional.empty());
    when(aiCommandRegistryService.resolveAny(anyString())).thenReturn(Optional.empty());
    return aiCommandRegistryService;
  }

  private EngineRequest request(String command) {
    return EngineRequest.builder()
        .command(command)
        .network("BOT_CLI_CLIENT")
        .fromSender("tester")
        .fromSenderId("tester")
        .replyTo("tester")
        .fromConnectionId(-1)
        .build();
  }

  private EngineRequest publicRequest(String command) {
    return EngineRequest.builder()
        .command(command)
        .network("IRCNet")
        .fromSender("tester")
        .fromSenderId("tester")
        .replyTo("#test")
        .echoToAlias("IRC-TEST")
        .fromConnectionId(1)
        .build();
  }

  private TheBotConfig publicAiConfig() {
    Channel channel = Channel.builder()
        .echoToAlias("IRC-TEST")
        .publicAiEnabled(true)
        .resolveUrls(true)
        .build();
    return TheBotConfig.builder()
        .botConfig(BotConfig.builder().botName("HokanDEV").build())
        .discordConfig(DiscordConfig.builder().channelList(List.of(channel)).build())
        .build();
  }

  private AiCommandDefinition aiCommand(String name, String usage, String description) {
    return new AiCommandDefinition(
        name,
        true,
        description,
        usage,
        List.of(),
        null,
        "Return final.",
        List.of(),
        3);
  }
}
