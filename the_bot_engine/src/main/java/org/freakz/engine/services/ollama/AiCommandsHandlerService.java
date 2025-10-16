package org.freakz.engine.services.ollama;

import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.dto.ai.AiCtrlResponse;
import org.freakz.engine.dto.ai.AiResponse;
import org.freakz.engine.dto.weather.WaterTemperatureResponse;
import org.freakz.engine.services.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;

@Service
@SpringServiceMethodHandler
public class AiCommandsHandlerService {

  private static final Logger log = LoggerFactory.getLogger(AiCommandsHandlerService.class);

  private final OllamaChatService ollamaChatService;

  public AiCommandsHandlerService(OllamaChatService ollamaChatService) {
    this.ollamaChatService = ollamaChatService;
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
    String promptMessage = "What is the current measured water temperature. Answer nothing else but \"YYY XXX °C\" YYY is measurement location and where XXX is temperature.";
    String imageUrl = "https://wwwi2.ymparisto.fi/i2/59/q5904450y/twlyhyt.png";

    try {
      String queryResponse = ollamaChatService.describeImageFromUrl(request.getEngineRequest(), "http://bot-ollama:11434", "qwen3-vl:235b-cloud", promptMessage, imageUrl,  network, channel, sentByNick, sentByRealName);
      response.setWaterTemperature("water temperature: "  + queryResponse);
      response.setStatus("OK:");
    } catch (MalformedURLException e) {
      log.error(e.getMessage(), e);
      response.setStatus("ERROR: "  + e.getMessage());
    }


    return response;
  }


}
