package org.freakz.engine.services.ai;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiClientFactory {

  private final RetryTemplate retryTemplate;

  @Value("${spring.ai.openai.api-key}")
  private String openApiKey;

  public AiClientFactory() {
    this.retryTemplate = RetryTemplate.builder()
        .maxAttempts(2)
        .fixedBackoff(500)
        .build();
  }

  public ChatClient openAiChatClient(String modelName) {
    OpenAiApi openAiApi = OpenAiApi.builder().apiKey(openApiKey).build();

    OpenAiChatModel aiChatModel = OpenAiChatModel.builder()
        .openAiApi(openAiApi)
        .retryTemplate(retryTemplate)
        .defaultOptions(OpenAiChatOptions.builder()
            .model(modelName)
            .build())
        .build();
    return ChatClient.create(aiChatModel);
  }

  public ChatClient createOllamaClient(String hostUrl, String modelName) {
    OllamaApi ollamaApi = OllamaApi.builder()
        .baseUrl(hostUrl)
        .build();

    OllamaChatModel chatModel = OllamaChatModel.builder()
        .ollamaApi(ollamaApi)
        .retryTemplate(retryTemplate)
        .defaultOptions(OllamaOptions.builder()
            .model(modelName)    // default model (user can override later)
            .build())
        .build();

    return ChatClient.create(chatModel);
  }
}
