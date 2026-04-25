package org.freakz.engine.services.ai;

import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.ai.AiResponse;
import org.freakz.engine.dto.weather.WaterTemperatureResponse;
import org.freakz.engine.services.ai.claw.OpenClawAiService;
import org.freakz.engine.services.api.ServiceMessageHandlerMethod;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.api.SpringServiceMethodHandler;
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

  private final ConfigService configService;

  private final OllamaAiService ollamaAiService;

  private final OpenAiService openAiService;

  private final OpenClawAiService openClawAiService;

  private final WaterTemperatureService waterTemperatureService;


  public AiCommandsHandlerService(ConfigService configService, OllamaAiService ollamaAiService, OpenAiService openAiService, OpenClawAiService openClawAiService, WaterTemperatureService waterTemperatureService) {
    this.configService = configService;
    this.ollamaAiService = ollamaAiService;
    this.openAiService = openAiService;
    this.openClawAiService = openClawAiService;
    this.waterTemperatureService = waterTemperatureService;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.AiService)
  public AiResponse handleServiceRequest(ServiceRequest request) {

    AiResponse aiResponse = AiResponse.builder().build();
    aiResponse.setStatus("OK: AI!");

    String message = request.getEngineRequest().getMessage();
    CommandArgs args = new CommandArgs(message);
    String queryMessage = args.joinArgs(0);

    openClawAiService.ask(request.getEngineRequest(), queryMessage);

    return aiResponse;
  }

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.WaterTemperatureService)
  public WaterTemperatureResponse handleWaterTemperatureServiceRequest(ServiceRequest request) {

    WaterTemperatureResponse response = WaterTemperatureResponse.builder().build();

    String network = request.getEngineRequest().getNetwork();
    String channel = request.getEngineRequest().getReplyTo();
    String sentByNick = request.getEngineRequest().getFromSender();
    String sentByRealName = request.getEngineRequest().getUser().getName();

    String place = request.getResults().getString(ARG_PLACE);

    boolean found = false;
    for (String key : waterTemperatureService.getDataMap().keySet()) {
      if (key.toLowerCase().contains(place.toLowerCase())) {
        WaterTemperatureData data = waterTemperatureService.getDataMap().get(key);
        String imageUrl = data.getWaterTemperatureChartImageUrl();
        try {

          String ollamaWaterHost =
              configService.getConfigValue("ollama.water.host", null, "http://bot-ollama:11434");
          String ollamaWaterModel =
              configService.getConfigValue("ollama.water.model", null, "qwen3-vl:235b-cloud");

          String promptMessage = "What is the current measured water temperature. Answer nothing else but XXX°C  where XXX is temperature.";
          String queryResponse;
          if (configService.getConfigBooleanValue("water.use.ollama", null, false)) {
            log.debug("Using OLLAMA to resolve water temperature");
            queryResponse = ollamaAiService.describeImageFromUrl(request.getEngineRequest(), ollamaWaterHost, ollamaWaterModel, promptMessage, imageUrl, network, channel, sentByNick, sentByRealName);
          } else {
            log.debug("Using OpenAI to resolve water temperature");
            queryResponse = openAiService.describeImageFromUrl(request.getEngineRequest(), ollamaWaterHost, ollamaWaterModel, promptMessage, imageUrl, network, channel, sentByNick, sentByRealName);
          }

          log.debug("queryResponse: {}", queryResponse);

          response.setWaterTemperature(key + " : " + queryResponse);
          response.setStatus("OK:");
        } catch (MalformedURLException e) {
          log.error(e.getMessage(), e);
          response.setStatus("ERROR: " + e.getMessage());
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
