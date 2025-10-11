package org.freakz.engine.services.ollama;

import org.freakz.engine.services.weather.foreca.ForecaWeatherService;
import org.freakz.engine.services.weather.weatherapi.WeatherAPIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class OllamaChatService {

  private static final Logger log = LoggerFactory.getLogger(OllamaChatService.class);

  @Value("classpath:/prompts/hokan-engine-template.st")
  private Resource hokanEngineTemplate;

  @Value("classpath:/prompts/hokan-prompt-template.st")
  private Resource hokanPromptTemplate;

  @Value("classpath:/prompts/hokan-system-template.st")
  private Resource hokanSystemTemplate;

  private final WeatherAPIService weatherAPIService;

  private final OllamaClientFactory factory;

  private final ChatMemory chatMemory = MessageWindowChatMemory.builder()
      .maxMessages(1000)
      .build(); // TODO

  public OllamaChatService(OllamaClientFactory factory, WeatherAPIService weatherAPIService) {
    this.factory = factory;
    this.weatherAPIService = weatherAPIService;
  }



  public String ask(String hostUrl, String modelName, String promptText, String network, String  channel, String sentByNick, String sentByRealName) {
    ChatClient client = factory.createClient(hostUrl, modelName);

    String chatId = String.format("%s-%s-%s", network, channel, sentByNick);
    log.debug("Using AI chatId: {}", chatId);

    StringBuilder sb = new StringBuilder();
    long start = System.currentTimeMillis();


    try {
      ToolCallback[] toolCallbacks = ToolCallbacks.from(new HokanToolCallBackFunctions(weatherAPIService));

      List<Message> memory = chatMemory.get(chatId);

      List<Message> messages = new ArrayList<>();
      messages.addAll(memory);



      SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(hokanSystemTemplate);
      Map<String, Object> systemPromptParameters = new HashMap<>();
      systemPromptParameters.put("network", network);
      systemPromptParameters.put("channel", channel);
      systemPromptParameters.put("sentByNick", sentByNick);
      systemPromptParameters.put("sentByRealName", sentByRealName);
      systemPromptParameters.put("localTime", LocalDateTime.now().toString());

      Message systemMessage = systemPromptTemplate.createMessage(systemPromptParameters);
      messages.addFirst(systemMessage);

      UserMessage userMessage = new UserMessage(promptText);
      messages.add(userMessage);

      Prompt prompt = new Prompt(messages,  OllamaOptions.builder()
          .toolCallbacks(toolCallbacks)
          .model(modelName) // "qwen3:30b-a3b"
          .temperature(0.4)
          .build()
      );

      ChatResponse response = client.prompt(prompt).call().chatResponse();
      Objects.requireNonNull(response).getResults().forEach(g -> {
        sb.append(g.getOutput().getText());
      });

      chatMemory.add(chatId, List.of(userMessage, new AssistantMessage(sb.toString())));

    } catch (Exception e) {
      log.error("Error while sending chat request: {}", e.getMessage());
      sb.append("ERROR: ").append(e.getMessage());
    }

    long end = System.currentTimeMillis();
    long duration = end - start;
    log.debug("Done sending chat request, handle took {} ms", duration);

    return sb.toString();
  }

}
