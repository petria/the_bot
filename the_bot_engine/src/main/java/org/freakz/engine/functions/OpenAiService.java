package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.AiResponse;
import org.freakz.engine.services.api.*;
import org.freakz.engine.services.connections.ConnectionManagerService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@SpringServiceMethodHandler
public class OpenAiService {

    @Value("classpath:/prompts/hokan-prompt-template.st")
    private Resource hokanPromptTemplate;

    @Value("classpath:/prompts/hokan-system-template.st")
    private Resource hokanSystemTemplate;

    //    private final OpenAiChatClient chatClient;
    private final ChatClient chatClient;
    private final ConfigService configService;

    private final ConnectionManagerService connectionManagerService;

    public OpenAiService(ChatClient.Builder builder, ConfigService configService, ConnectionManagerService connectionManagerService) {
        ChatClient chatClient1;
        chatClient1 = chatClient1 = builder.build();
        this.chatClient = chatClient1;
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

        promptParameters.put("input", message);
        promptParameters.put("bot_name", configService.readBotConfig().getBotConfig().getBotName());
        Message promptMessage = promptTemplate.createMessage(promptParameters);

        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()

                .withFunctions(Set.of("currentWeatherFunction", "myCurrentLocationFunction", "ircChatInfoFunction")) // "currentTimeFunction",
                .build();

        ChatClient.ChatClientRequest.CallResponseSpec call1 = chatClient.prompt().options(chatOptions).system(systemMessage.getContent()).user(promptMessage.getContent()).call();
//        ChatClient.ChatClientPromptRequest prompt = chatClient.prompt(new Prompt(List.of(systemMessage, promptMessage)));

//        ChatClient.ChatClientRequest.CallPromptResponseSpec call = chatClient.prompt(new Prompt(List.of(systemMessage, promptMessage))).call();
//        ChatResponse response = chatClient.call(new Prompt(List.of(systemMessage, promptMessage), chatOptions));

        return call1.content();
    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiService)
    public <T extends ServiceResponse> AiResponse handleServiceRequest(ServiceRequest request) {

//        GetConnectionMapResponse connectionsMap = connectionManagerService.getConnectionsMap();

        AiResponse aiResponse = AiResponse.builder().build();
        aiResponse.setStatus("OK: AI!");

        String message = request.getEngineRequest().getMessage();
        CommandArgs args = new CommandArgs(message);
        String queryMessage = args.joinArgs(0);

        String network = request.getEngineRequest().getNetwork();
        String channel = request.getEngineRequest().getNetwork();
        String sentByNick = request.getEngineRequest().getFromSender();
        String sentByRealName = request.getEngineRequest().getUser().getName();

//        String channel = request.getEngineRequest().get


        String queryResponse = queryAiWithTemplate(queryMessage, network, channel, sentByNick, sentByRealName, request);
        aiResponse.setResult(queryResponse);
        return aiResponse;
    }

}