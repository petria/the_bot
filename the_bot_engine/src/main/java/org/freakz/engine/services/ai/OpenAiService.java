package org.freakz.engine.services.ai;

import org.freakz.common.model.engine.EngineRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.UnknownContentTypeException;
import tools.jackson.databind.json.JsonMapper;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpenAiService {

  private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

  private final AiClientFactory factory;
  private final JsonMapper jsonMapper;

  public OpenAiService(AiClientFactory factory, JsonMapper jsonMapper) {
    this.factory = factory;
    this.jsonMapper = jsonMapper;
  }


  public String describeImageFromUrl(EngineRequest engineRequest, String hostUrl, String modelName, String promptText, String imageUrl, String network, String channel, String sentByNick, String sentByRealName) throws MalformedURLException {

    String response;

    ChatClient client = factory.openAiChatClient("chatgpt-4o-latest");  //openAiChatClient;
    long start = 0L;
    log.debug("Image URL: {}", imageUrl);
    var urlResource = new UrlResource(imageUrl);
    Media media = new Media(MimeTypeUtils.IMAGE_PNG, urlResource);
    try {
      UserMessage build = UserMessage.builder().media(media).text(promptText).build();
      log.debug("Sending prompt to OpenAI api.. ");
      start = System.currentTimeMillis();
      ChatResponse chatResponse = client.prompt(new Prompt(build)).call().chatResponse();
      if (chatResponse != null) {
        response = chatResponse.getResult().getOutput().getText();
      } else {
        response = "N/A";
      }
      log.debug("... image Done1");

    } catch (UnknownContentTypeException e) {
      log.debug("... image Done2");
      // TODO fix this stupid way to get data
      String responseBodyAsString = e.getResponseBodyAsString();
//      ObjectMapper mapper = new ObjectMapper();
      try {
        Map map = jsonMapper.readValue(responseBodyAsString, HashMap.class);
        if (map.containsKey("message")) {
          map = (HashMap) map.get("message");
          response = (String) map.get("content");
        } else {
          response = "N/A";
        }
      } catch (Exception ex) {
        response = "ERROR: " + ex.getMessage();
      }

    }
    log.debug("... query took {} ms", System.currentTimeMillis() - start);
    return response;
//    return "TODO";
  }

}
