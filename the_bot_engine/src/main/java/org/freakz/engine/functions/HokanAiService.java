package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.ai.AiCtrlResponse;
import org.freakz.engine.dto.ai.AiResponse;
import org.freakz.engine.services.api.*;
import org.freakz.engine.services.connections.ConnectionManagerService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


//@Service
@Slf4j
//@SpringServiceMethodHandler
public class HokanAiService {

  @Value("classpath:/prompts/hokan-prompt-template.st")
  private Resource hokanPromptTemplate;

  @Value("classpath:/prompts/hokan-system-template.st")
  private Resource hokanSystemTemplate;

  @Value("classpath:/prompts/hokan-engine-template.st")
  private Resource hokanEngineTemplate;

  private final ChatClient chatClient;
  private final ConfigService configService;

  private final ConnectionManagerService connectionManagerService;

  public HokanAiService(
      ChatClient.Builder builder,
      ConfigService configService,
      ConnectionManagerService connectionManagerService) {

    ChatMemory chatMemory
        = MessageWindowChatMemory.builder()
        .maxMessages(1000)
        .build();

    this.chatClient
        = builder
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
//        .defaultToolCallbacks(toolCallBacks)
        .build();

//        builder
//            .defaultAdvisors(new MessageChatMemoryAdvisor(inMemoryChatMemory))
//                .defaultFunctions("myCurrentLocationFunction", "ircChatInfoFunction", "handeEngineCommandFunction")
//            .build();
    this.configService = configService;
    this.connectionManagerService = connectionManagerService;
  }

  public String queryAiWithTemplate(
      String message,
      String network,
      String channel,
      String sentByNick,
      String sentByRealName,
      ServiceRequest request) {

    SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(hokanSystemTemplate);
    Map<String, Object> systemPromptParameters = new HashMap<>();
    systemPromptParameters.put("network", network);
    systemPromptParameters.put("channel", channel);
    systemPromptParameters.put("sentByNick", sentByNick);
    systemPromptParameters.put("sentByRealName", sentByRealName);
    systemPromptParameters.put("localTime", LocalDateTime.now().toString());
    Message systemMessage = systemPromptTemplate.createMessage(systemPromptParameters);

    SystemPromptTemplate systemPromptTemplate2 = new SystemPromptTemplate(hokanEngineTemplate);
    Message systemMessage2 = systemPromptTemplate2.createMessage(systemPromptParameters);

    PromptTemplate promptTemplate = new PromptTemplate(hokanPromptTemplate);
    Map<String, Object> promptParameters = new HashMap<>();

    switch (request.getEngineRequest().getNetwork()) {
      case "IRCNet":
        if (request.getEngineRequest().isPrivateChannel()) {
          promptParameters.put("answerMaxLengthCharacters", 2500);
        } else {
          promptParameters.put("answerMaxLengthCharacters", 400);
        }
        break;
      default:
        promptParameters.put("answerMaxLengthCharacters", Long.MAX_VALUE);
    }
    String chatId = String.format("%s-%s-%s", network, channel, sentByNick);
    log.debug("Using AI chatId: {}", chatId);
    promptParameters.put("input", message);
    promptParameters.put("bot_name", configService.readBotConfig().getBotConfig().getBotName());
    Message promptMessage = promptTemplate.createMessage(promptParameters);
    String content
        = this.chatClient.prompt()
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
        .user(promptMessage.getText())
//        .system(systemMessage.getText())
//        .system(systemMessage2.getText())
        .call().content();

    return content;
  }

//  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiService)
  public <T extends ServiceResponse> AiResponse handleServiceRequest(ServiceRequest request) {

    //        GetConnectionMapResponse connectionsMap =
    // connectionManagerService.getConnectionsMap(); TODO

    AiResponse aiResponse = AiResponse.builder().build();
    aiResponse.setStatus("OK: AI!");

    String message = request.getEngineRequest().getMessage();
    CommandArgs args = new CommandArgs(message);
    String queryMessage = args.joinArgs(0);

    String network = request.getEngineRequest().getNetwork();
    String channel = request.getEngineRequest().getReplyTo();
    String sentByNick = request.getEngineRequest().getFromSender();
    String sentByRealName = request.getEngineRequest().getUser().getName();

    String queryResponse =
        queryAiWithTemplate(queryMessage, network, channel, sentByNick, sentByRealName, request);
    aiResponse.setResult(queryResponse);
    return aiResponse;
  }

//  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiCtrlService)
  public <T extends ServiceResponse> AiCtrlResponse handleAiCtlServiceRequest(
      ServiceRequest request) {
    AiCtrlResponse aiResponse = AiCtrlResponse.builder().build();
    aiResponse.setStatus("OK: AiCtl!");
    return aiResponse;
  }
}
