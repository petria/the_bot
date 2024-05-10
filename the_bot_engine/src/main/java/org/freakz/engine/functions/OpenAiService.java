package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.AiResponse;
import org.freakz.engine.services.api.*;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@SpringServiceMethodHandler
public class OpenAiService {

    private final OpenAiChatClient chatClient;
    private final ConfigService configService;

    public OpenAiService(OpenAiChatClient chatClient, ConfigService configService) {
        this.chatClient = chatClient;
        this.configService = configService;
    }

    public void testImageGeneration() {
/*        ImageResponse response = openaiImageClient.call(
                new ImagePrompt("A light cream colored mini golden doodle",
                        OpenAiImageOptions.builder()
                                .withQuality("hd")
                                .withN(4)
                                .withHeight(1024)
                                .withWidth(1024).build())

        );*/
    }


    public String queryAi(String message) {
        SystemMessage systemMessage = new SystemMessage("You are helpful AI chat bot which answers questions coming from chat text channels. You are connected to IRC, Discord and Telegram networks. Your name in the chat is " + configService.readBotConfig().getBotConfig().getBotName());
        log.debug("Query ai: {}", message);
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .withFunctions(Set.of("currentWeatherFunction", "currentTimeFunction", "myCurrentLocationFunction", "ircChatInfoFunction"))
                .build();
        UserMessage userMessage = new UserMessage(message);
        ChatResponse response = chatClient.call(new Prompt(List.of(systemMessage, userMessage), chatOptions));
//        Generation generation = this.chatClient.call(prompt, chatOptions).getResult();
//        log.debug("Query ai done, generation: {}", generation.toString());
        return response.getResult().getOutput().getContent();
    }


    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiService)
    public <T extends ServiceResponse> AiResponse handleServiceRequest(ServiceRequest request) {
        AiResponse aiResponse = AiResponse.builder().build();
        aiResponse.setStatus("OK: AI!");

        String message = request.getEngineRequest().getMessage();
        CommandArgs args = new CommandArgs(message);
        String queryMessage = args.joinArgs(0);
        aiResponse.setResult(queryAi(queryMessage));
        return aiResponse;
    }

}