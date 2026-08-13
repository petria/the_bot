package org.freakz.engine.commands.handlers.admin;

import org.freakz.common.model.connectionmanager.IrcModeSetResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.irc.IrcModeManagementService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ModeSetCmdTest {

  @Test
  void modeSetReportsSuccessfulChange() throws Exception {
    IrcModeManagementService service = mock(IrcModeManagementService.class);
    when(service.setModes(eq("IRC-TEST"), eq("+st"), any())).thenReturn(
        new IrcModeSetResponse("IRC-TEST", "#test", true, "+st", null));
    BotEngine botEngine = mock(BotEngine.class);
    when(botEngine.getIrcModeManagementService()).thenReturn(service);
    ModeSetCmd command = new ModeSetCmd();
    command.setBotEngine(botEngine);
    command.abstractInitCommandOptions();

    String reply = command.executeCommand(
        EngineRequest.builder().chatProtocol("irc").echoToAlias("IRC-TEST").build(),
        command.getJsap().parse("+st"));

    assertThat(reply).isEqualTo("IRC channel modes updated.");
  }
}
