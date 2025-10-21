package org.freakz.engine.services.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiBeanConfig {

  @Bean
  @Qualifier("openAiChatClient")
  public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
    ChatClient.Builder builder = ChatClient.builder(chatModel);
    ChatClient client = builder.defaultOptions(OpenAiChatOptions.builder().model("chatgpt-4o-latest").build()).build();
    return client;
  }

}
