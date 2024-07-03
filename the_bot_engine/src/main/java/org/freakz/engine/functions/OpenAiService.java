package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.AiResponse;
import org.freakz.engine.services.api.*;
import org.freakz.engine.services.connections.ConnectionManagerService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
@Slf4j
@SpringServiceMethodHandler
public class OpenAiService {


    @Value("classpath:/prompts/hokan-prompt-template.st")
    private Resource hokanPromptTemplate;

    @Value("classpath:/prompts/hokan-system-template.st")
    private Resource hokanSystemTemplate;

    private final InMemoryChatMemory inMemoryChatMemory;

    private final ChatClient chatClient;
    private final ConfigService configService;

    private final ConnectionManagerService connectionManagerService;

    public OpenAiService(ChatClient.Builder builder, ConfigService configService, ConnectionManagerService connectionManagerService) {
        this.inMemoryChatMemory = new InMemoryChatMemory();
        this.chatClient = builder
                .defaultAdvisors(new MessageChatMemoryAdvisor(inMemoryChatMemory))
                .defaultFunctions("currentWeatherFunction", "myCurrentLocationFunction", "ircChatInfoFunction")
                .build();
        this.configService = configService;
        this.connectionManagerService = connectionManagerService;
    }


    public String queryAiWithTemplate(String message, String network, String channel, String sentByNick, String sentByRealName, ServiceRequest request) {

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(hokanSystemTemplate);
        Map<String, Object> systemPromptParameters = new HashMap<>();
        systemPromptParameters.put("network", network);
        systemPromptParameters.put("channel", channel);
        systemPromptParameters.put("sentByNick", sentByNick);
        systemPromptParameters.put("sentByRealName", sentByRealName);
        systemPromptParameters.put("localTime", LocalDateTime.now().toString());
        Message systemMessage = systemPromptTemplate.createMessage(systemPromptParameters);

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

        promptParameters.put("input", message);
        promptParameters.put("bot_name", configService.readBotConfig().getBotConfig().getBotName());
        Message promptMessage = promptTemplate.createMessage(promptParameters);
        ChatClient.ChatClientRequest.CallResponseSpec call1
                = chatClient.prompt()
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
                )
                .system(systemMessage.getContent())
                .user(promptMessage.getContent())
                .call();

        return call1.content();
    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiService)
    public <T extends ServiceResponse> AiResponse handleServiceRequest(ServiceRequest request) {

//        GetConnectionMapResponse connectionsMap = connectionManagerService.getConnectionsMap(); TODO

        AiResponse aiResponse = AiResponse.builder().build();
        aiResponse.setStatus("OK: AI!");

        String message = request.getEngineRequest().getMessage();
        CommandArgs args = new CommandArgs(message);
        String queryMessage = args.joinArgs(0);

        String network = request.getEngineRequest().getNetwork();
        String channel = request.getEngineRequest().getNetwork();
        String sentByNick = request.getEngineRequest().getFromSender();
        String sentByRealName = request.getEngineRequest().getUser().getName();

        String queryResponse = queryAiWithTemplate(queryMessage, network, channel, sentByNick, sentByRealName, request);
        aiResponse.setResult(queryResponse);
        return aiResponse;
    }

}