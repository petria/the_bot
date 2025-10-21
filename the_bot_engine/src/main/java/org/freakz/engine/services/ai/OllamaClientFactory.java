package org.freakz.engine.services.ai;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

@Service
public class OllamaClientFactory {

  private final RetryTemplate retryTemplate;

  public OllamaClientFactory() {
    this.retryTemplate = RetryTemplate.builder()
        .maxAttempts(2)
        .fixedBackoff(500)
        .build();
  }

  public ChatClient createClient(String hostUrl, String modelName) {
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
