package org.freakz.engine.commands;

import org.freakz.common.model.botconfig.BotConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.engine.commands.ai.AiCommandRegistryService;
import org.freakz.engine.commands.output.ReplyOutputService;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.HokanServices;
import org.freakz.engine.services.ai.commands.HermesAiCommandService;
import org.freakz.engine.services.notifications.PrivateChatAlertService;
import org.freakz.engine.services.urls.UrlMetadataService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
  void countsPermissionDeniedCommands() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    BotEngine botEngine = botEngine(statsService);

    botEngine.handleEngineRequest(request("!buildinfo"), true);

    assertThat(statsService.getCommandInvocationCount("main::buildinfo")).isEqualTo(1);
    assertThat(statsService.getProviderInvocationCount("main")).isEqualTo(1);
  }

  @Test
  void doesNotCountUnknownCommands() throws Exception {
    CommandInvocationStatsService statsService = new CommandInvocationStatsService((io.micrometer.core.instrument.MeterRegistry) null);
    BotEngine botEngine = botEngine(statsService);

    botEngine.handleEngineRequest(request("!doesnotexist"), true);

    assertThat(statsService.getProviderInvocationCount("main")).isZero();
  }

  private BotEngine botEngine(CommandInvocationStatsService statsService) throws Exception {
    ConfigService configService = mock(ConfigService.class);
    when(configService.getActiveProfile()).thenReturn("DEV");
    when(configService.readBotConfig()).thenReturn(
        TheBotConfig.builder()
            .botConfig(BotConfig.builder().botName("HokanDEV").build())
            .build());

    UsersService usersService = mock(UsersService.class);
    User unknownUser = User.builder().username("unknown").permissions(List.of()).build();
    unknownUser.setId(-1L);
    when(usersService.findAll()).thenReturn(List.of());
    when(usersService.getNotKnownUser()).thenReturn(unknownUser);
    AiCommandRegistryService aiCommandRegistryService = mock(AiCommandRegistryService.class);
    when(aiCommandRegistryService.resolve(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());

    return new BotEngine(
        new AccessService(usersService),
        mock(HokanServices.class),
        configService,
        mock(UrlMetadataService.class),
        null,
        mock(PrivateChatAlertService.class),
        mock(ReplyOutputService.class),
        statsService,
        aiCommandRegistryService,
        mock(HermesAiCommandService.class));
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
}
