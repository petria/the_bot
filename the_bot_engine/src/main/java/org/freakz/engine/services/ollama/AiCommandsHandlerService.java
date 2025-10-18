package org.freakz.engine.services.ollama;

import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.dto.ai.AiCtrlResponse;
import org.freakz.engine.dto.ai.AiResponse;
import org.freakz.engine.dto.weather.WaterTemperatureResponse;
import org.freakz.engine.services.api.*;
import org.freakz.engine.services.weather.water.WaterTemperatureData;
import org.freakz.engine.services.weather.water.WaterTemperatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

@Service
@SpringServiceMethodHandler
public class AiCommandsHandlerService {

  private static final Logger log = LoggerFactory.getLogger(AiCommandsHandlerService.class);

  private final OllamaChatService ollamaChatService;

  private final WaterTemperatureService waterTemperatureService;

  public AiCommandsHandlerService(OllamaChatService ollamaChatService, WaterTemperatureService waterTemperatureService) {
    this.ollamaChatService = ollamaChatService;
    this.waterTemperatureService = waterTemperatureService;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiService)
  public AiResponse handleServiceRequest(ServiceRequest request) {


    AiResponse aiResponse = AiResponse.builder().build();
    aiResponse.setStatus("OK: AI!");

    String message = request.getEngineRequest().getMessage();
    CommandArgs args = new CommandArgs(message);
    String queryMessage = args.joinArgs(0);

    String network = request.getEngineRequest().getNetwork();
    String channel = request.getEngineRequest().getReplyTo();
    String sentByNick = request.getEngineRequest().getFromSender();
    String sentByRealName = request.getEngineRequest().getUser().getName();

    String queryResponse = ollamaChatService.ask(request.getEngineRequest(), "http://bot-ollama:11434", "llama3.1:8b", queryMessage, network, channel, sentByNick, sentByRealName);

    aiResponse.setResult(queryResponse);
    return aiResponse;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiCtrlService)
  public AiCtrlResponse handleAiCtlServiceRequest(
      ServiceRequest request) {
    AiCtrlResponse aiResponse = AiCtrlResponse.builder().build();
    aiResponse.setStatus("OK: AiCtl!");
    return aiResponse;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.WaterTemperatureService)
  public WaterTemperatureResponse handleWaterTemperatureServiceRequest(ServiceRequest request) {
    WaterTemperatureResponse response = WaterTemperatureResponse.builder().build();

    String network = request.getEngineRequest().getNetwork();
    String channel = request.getEngineRequest().getReplyTo();
    String sentByNick = request.getEngineRequest().getFromSender();
    String sentByRealName = request.getEngineRequest().getUser().getName();
//    String promptMessage = "What is the current measured water temperature. Also find measurement date and time from lower right corner of image. Answer nothing else but \"DDD - YYY XXX °C\" DDD is date and time, YYY is measurement location and where XXX is temperature.";
    String promptMessage = "What is the current measured water temperature. Also find measurement date and time from lower right corner of image. Answer nothing else but \"XXX °C\"  where XXX is temperature.";
//    String imageUrl = "https://wwwi2.ymparisto.fi/i2/65/l653941026y/twlyhyt.png";
//                     https://wwwi2.ymparisto.fi/i2/59/q5904450y/twlyhyt.png
    String place = request.getResults().getString(ARG_PLACE);
    boolean found = false;
    for (String key : waterTemperatureService.getDataMap().keySet()) {
      if (key.toLowerCase().contains(place.toLowerCase())) {
        WaterTemperatureData data = waterTemperatureService.getDataMap().get(key);
        String imageUrl = data.getWaterTemperatureChartImageUrl();
        try {
//          log.info("Query chart image from URL: {}", imageUrl);
          // qwen2.5vl:7b
          // qwen3-vl:235b-cloud
          // llama3.2-vision:latest
//          String ollamaHost = "http://192.168.0.111:11434";//"http://bot-ollama:11434";
          String ollamaHost = "http://bot-ollama:11434";
          String ollamaModel = "qwen2.5vl:32b";
          String queryResponse = ollamaChatService.describeImageFromUrl(request.getEngineRequest(), ollamaHost, ollamaModel, promptMessage, imageUrl,  network, channel, sentByNick, sentByRealName);
          response.setWaterTemperature(key + " : "  + queryResponse);
          response.setStatus("OK:");
        } catch (MalformedURLException e) {
          log.error(e.getMessage(), e);
          response.setStatus("ERROR: "  + e.getMessage());
        }
        found = true;
        break;
      }
    }
    if (!found) {
      response.setStatus("NOK: not found");
    }

    return response;
  }


}
