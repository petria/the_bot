package org.freakz.engine.services.ai;

import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.AiResponse;
import org.freakz.engine.services.api.*;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@SpringServiceMethodHandler
public class OpenAiService {

    private final OpenAiChatClient chatClient;

    public OpenAiService(ConfigService configService) {

        var openApiKey = configService.readBotConfig().getBotConfig().getOpenAiApiKey();
        log.debug("Init OpenAI client: {}", openApiKey);

        var openAiApi = new OpenAiApi(openApiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel("gpt-3.5-turbo")
                .withTemperature(0.4f)
                .withMaxTokens(200)
                .build();

        this.chatClient = new OpenAiChatClient(openAiApi, options);

        log.debug("Init OpenAI client done: {}", this.chatClient.toString());

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
        Prompt prompt = new Prompt(message);
        log.debug("Query ai: {}", message);
        Generation generation = this.chatClient.call(prompt).getResult();
        log.debug("Query ai done, generation: {}", generation.toString());
        return generation.getOutput().getContent();
    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiService)
    public <T extends ServiceResponse> AiResponse handleServiceRequest(ServiceRequest request) {
        AiResponse aiResponse = AiResponse.builder().build();
        aiResponse.setStatus("OK: AI!");
        aiResponse.setResult(queryAi(request.getEngineRequest().getMessage()));
        return aiResponse;
    }

}