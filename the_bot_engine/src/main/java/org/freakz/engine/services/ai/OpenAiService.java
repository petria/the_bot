package org.freakz.engine.services.ai;

import org.freakz.engine.config.ConfigService;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService {

    private final OpenAiChatClient chatClient;

    public OpenAiService(ConfigService configService) {

        var openApiKey = configService.readBotConfig().getBotConfig().getOpenAiApiKey();
        var openAiApi = new OpenAiApi(openApiKey);
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel("gpt-3.5-turbo")
                .withTemperature(0.4f)
                .withMaxTokens(200)
                .build();

        this.chatClient = new OpenAiChatClient(openAiApi, options);

    }

    public String queryAi(String message) {
        Prompt prompt = new Prompt(message);
        Generation generation = this.chatClient.call(prompt).getResult();
        return generation.getOutput().getContent();
    }

}
