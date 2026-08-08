package org.freakz.engine.commands.handlers.admin;

import org.freakz.common.model.connectionmanager.IrcTopicSetResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.irc.IrcTopicManagementService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TopicCommandTest {

  @Test
  void topicSetRequiresAnIrcChannelRequest() throws Exception {
    TopicSetCmd command = new TopicSetCmd();
    command.abstractInitCommandOptions();

    assertThat(command.executeCommand(
        EngineRequest.builder().chatProtocol("telegram").build(),
        command.getJsap().parse("hello"))).isNull();
  }

  @Test
  void topicSetReportsSuccessfulChange() throws Exception {
    IrcTopicManagementService topicService = mock(IrcTopicManagementService.class);
    when(topicService.setTopic(anyString(), anyString(), any())).thenReturn(
        new IrcTopicSetResponse("IRC-TEST", "#test", true, false, "hello world", null));
    BotEngine botEngine = mock(BotEngine.class);
    when(botEngine.getIrcTopicManagementService()).thenReturn(topicService);
    TopicSetCmd command = new TopicSetCmd();
    command.setBotEngine(botEngine);
    command.abstractInitCommandOptions();

    String reply = command.executeCommand(
        EngineRequest.builder().chatProtocol("irc").echoToAlias("IRC-TEST").build(),
        command.getJsap().parse("hello world"));

    assertThat(reply).isEqualTo("Topic set.");
    verify(topicService).setTopic(eq("IRC-TEST"), eq("hello world"), any());
  }

  @Test
  void topicAddUsesTheGuardedTopic() throws Exception {
    IrcTopicManagementService topicService = mock(IrcTopicManagementService.class);
    when(topicService.guardedTopic("IRC-TEST")).thenReturn("current topic");
    when(topicService.setTopic(eq("IRC-TEST"), eq("current topic. new info"), any())).thenReturn(
        new IrcTopicSetResponse("IRC-TEST", "#test", true, false, "current topic. new info", null));
    BotEngine botEngine = mock(BotEngine.class);
    when(botEngine.getIrcTopicManagementService()).thenReturn(topicService);
    TopicAddCmd command = new TopicAddCmd();
    command.setBotEngine(botEngine);
    command.abstractInitCommandOptions();

    String reply = command.executeCommand(
        EngineRequest.builder().chatProtocol("irc").echoToAlias("IRC-TEST").build(),
        command.getJsap().parse(". new info"));

    assertThat(reply).isEqualTo("Topic updated.");
  }
}
