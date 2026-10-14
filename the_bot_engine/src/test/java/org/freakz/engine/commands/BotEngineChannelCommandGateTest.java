package org.freakz.engine.commands;

import org.freakz.common.model.botconfig.BotConfig;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.DiscordConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.engine.commands.ai.AiCommandRegistryService;
import org.freakz.engine.commands.output.ReplyOutputService;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.config.ConfiguredChannelResolver;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.HokanServices;
import org.freakz.engine.services.ProcessingIndicatorService;
import org.freakz.engine.services.ai.commands.HermesAiCommandService;
import org.freakz.engine.services.console.ConsoleOutputService;
import org.freakz.engine.services.notifications.PrivateChatAlertService;
import org.freakz.engine.services.urls.UrlResolutionService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Two bot instances can sit in the same chat channel. Explicit commands must only be answered by
 * the instance that has the channel configured and has not disabled commands for that channel.
 */
class BotEngineChannelCommandGateTest {

  @Test
  void answersCommandsInConfiguredChannel() throws Exception {
    CommandInvocationStatsService statsService = newStats();
    BotEngine botEngine = botEngine(statsService, configWithChannel(
        Channel.builder().echoToAlias("DISCORD-GENERAL").commandsEnabled(true).build()));

    botEngine.handleEngineRequest(channelRequest("!ping"), true);

    assertThat(statsService.getCommandInvocationCount("main::ping")).isEqualTo(1);
  }

  @Test
  void answersCommandsInLegacyChannelConfigWithoutTheFlag() throws Exception {
    CommandInvocationStatsService statsService = newStats();
    BotEngine botEngine = botEngine(statsService, configWithChannel(
        Channel.builder().echoToAlias("DISCORD-GENERAL").build()));

    botEngine.handleEngineRequest(channelRequest("!ping"), true);

    assertThat(statsService.getCommandInvocationCount("main::ping")).isEqualTo(1);
  }

  @Test
  void staysSilentInConfiguredChannelWithCommandsDisabled() throws Exception {
    CommandInvocationStatsService statsService = newStats();
    BotEngine botEngine = botEngine(statsService, configWithChannel(
        Channel.builder().echoToAlias("DISCORD-GENERAL").commandsEnabled(false).build()));

    String reply = botEngine.handleEngineRequest(channelRequest("!ping"), true).getReplyMessage();

    assertThat(reply).isNull();
    assertThat(statsService.getCommandInvocationCount("main::ping")).isZero();
    assertThat(statsService.getProviderInvocationCount("main")).isZero();
  }

  @Test
  void staysSilentInChannelThatThisInstanceHasNotConfigured() throws Exception {
    CommandInvocationStatsService statsService = newStats();
    BotEngine botEngine = botEngine(statsService, configWithChannel(
        Channel.builder().echoToAlias("DISCORD-OTHER").commandsEnabled(true).build()));

    String reply = botEngine.handleEngineRequest(channelRequest("!ping"), true).getReplyMessage();

    assertThat(reply).isNull();
    assertThat(statsService.getCommandInvocationCount("main::ping")).isZero();
  }

  @Test
  void answersCommandsInPrivateChatWithoutChannelConfig() throws Exception {
    CommandInvocationStatsService statsService = newStats();
    BotEngine botEngine = botEngine(statsService, configWithChannel(
        Channel.builder().echoToAlias("DISCORD-GENERAL").commandsEnabled(false).build()));
    EngineRequest request = channelRequest("!ping");
    request.setPrivateChannel(true);
    request.setEchoToAlias("PRIVATE-DISCORD-42");

    botEngine.handleEngineRequest(request, true);

    assertThat(statsService.getCommandInvocationCount("main::ping")).isEqualTo(1);
  }

  @Test
  void answersCommandsFromNonChatCallersWithoutChannelConfig() throws Exception {
    CommandInvocationStatsService statsService = newStats();
    BotEngine botEngine = botEngine(statsService, TheBotConfig.builder()
        .botConfig(BotConfig.builder().botName("HokanDEV").build())
        .build());
    EngineRequest request = EngineRequest.builder()
        .command("!ping")
        .network("BOT_CLI_CLIENT")
        .fromSender("tester")
        .fromSenderId("tester")
        .replyTo("tester")
        .fromConnectionId(-1)
        .build();

    botEngine.handleEngineRequest(request, true);

    assertThat(statsService.getCommandInvocationCount("main::ping")).isEqualTo(1);
  }

  private CommandInvocationStatsService newStats() {
    return new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
  }

  private TheBotConfig configWithChannel(Channel channel) {
    return TheBotConfig.builder()
        .botConfig(BotConfig.builder().botName("HokanDEV").build())
        .discordConfig(DiscordConfig.builder().channelList(List.of(channel)).build())
        .build();
  }

  private EngineRequest channelRequest(String command) {
    return EngineRequest.builder()
        .command(command)
        .network("Discord")
        .fromSender("tester")
        .fromSenderId("tester")
        .replyTo("#general")
        .echoToAlias("DISCORD-GENERAL")
        .fromConnectionId(1)
        .build();
  }

  private BotEngine botEngine(CommandInvocationStatsService statsService, TheBotConfig botConfig)
      throws Exception {
    ConfigService configService = mock(ConfigService.class);
    when(configService.getActiveProfile()).thenReturn("DEV");
    when(configService.readBotConfig()).thenReturn(botConfig);

    UsersService usersService = mock(UsersService.class);
    User unknownUser = User.builder().username("unknown").permissions(List.of()).build();
    unknownUser.setId(-1L);
    when(usersService.findAll()).thenReturn(List.of());
    when(usersService.getNotKnownUser()).thenReturn(unknownUser);

    AiCommandRegistryService aiCommandRegistryService = mock(AiCommandRegistryService.class);
    when(aiCommandRegistryService.resolve(anyString())).thenReturn(Optional.empty());
    when(aiCommandRegistryService.resolveAny(anyString())).thenReturn(Optional.empty());

    return new BotEngine(
        new AccessService(usersService),
        mock(HokanServices.class),
        configService,
        mock(UrlResolutionService.class),
        null,
        mock(PrivateChatAlertService.class),
        mock(ReplyOutputService.class),
        statsService,
        aiCommandRegistryService,
        mock(HermesAiCommandService.class),
        new ConfiguredChannelResolver(),
        new ConsoleOutputService(),
        new ProcessingIndicatorService(null));
  }
}
