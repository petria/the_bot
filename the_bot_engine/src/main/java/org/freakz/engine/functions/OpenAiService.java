package org.freakz.engine.functions;

import lombok.extern.slf4j.Slf4j;
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

    public OpenAiService(OpenAiChatClient chatClient) {
        this.chatClient = chatClient;
/*        var openApiKey = configService.readBotConfig().getBotConfig().getOpenAiApiKey();
        log.debug("Init OpenAI client: {}", openApiKey);

        var openAiApi = new OpenAiApi(openApiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
//                .withFunctions(Set.of("WeatherInfo"))
                .withModel("gpt-4")
                .withTemperature(0.4f)
                .withMaxTokens(150)
                .build();

        this.chatClient = new OpenAiChatClient(openAiApi, options);

        log.debug("Init OpenAI client done: {}", this.chatClient.toString());*/

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
        SystemMessage systemMessage = new SystemMessage("You are helpful AI IRC bot which answers questions coming from IRC chat channels.");
        log.debug("Query ai: {}", message);
        OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .withFunctions(Set.of("currentWeatherFunction", "currentTimeFunction", "myCurrentLocationFunction"))
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
        aiResponse.setResult(queryAi(request.getEngineRequest().getMessage()));
        return aiResponse;
    }

}