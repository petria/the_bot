package org.freakz.engine.services.ai;

import org.freakz.common.model.env.SysEnvValue;
import org.freakz.engine.commands.util.CommandArgs;
import org.freakz.engine.data.service.EnvValuesService;
import org.freakz.engine.dto.ai.AiCtrlResponse;
import org.freakz.engine.dto.ai.AiResponse;
import org.freakz.engine.dto.weather.WaterTemperatureResponse;
import org.freakz.engine.services.api.ServiceMessageHandlerMethod;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.api.SpringServiceMethodHandler;
import org.freakz.engine.services.weather.water.WaterTemperatureData;
import org.freakz.engine.services.weather.water.WaterTemperatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

import java.net.MalformedURLException;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

@Service
@SpringServiceMethodHandler
public class AiCommandsHandlerService {

  private static final Logger log = LoggerFactory.getLogger(AiCommandsHandlerService.class);

  private final EnvValuesService envValuesService;

  private final OllamaAiService ollamaAiService;

  private final OpenAiService openAiService;

  private final OpenClawAiService openClawAiService;

  private final WaterTemperatureService waterTemperatureService;


  public AiCommandsHandlerService(EnvValuesService envValuesService, OllamaAiService ollamaAiService, OpenAiService openAiService, OpenClawAiService openClawAiService, WaterTemperatureService waterTemperatureService) {
    this.envValuesService = envValuesService;
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

    String network = request.getEngineRequest().getNetwork();
    String channel = request.getEngineRequest().getReplyTo();
    String sentByNick = request.getEngineRequest().getFromSender();
    String sentByRealName = request.getEngineRequest().getUser().getName();

    String backend = envValuesService.getKeyValueOrDefault("aiBackend", "ollama").toLowerCase(Locale.ROOT);
    boolean fallbackToOllama = envValuesService.getKeyValueBooleanOrDefault("openclawFallbackToOllama", true);

    String hostUrl = envValuesService.getKeyValueOrDefault("ollamaHost", "http://bot-ollama:11434");
    String modelName = envValuesService.getKeyValueOrDefault("ollamaChatModel", "qwen2.5:14b");

    String queryResponse;
    if ("openclaw".equals(backend)) {
      OpenClawAiService.OpenClawAskResult askResult = openClawAiService.ask(request.getEngineRequest(), queryMessage);
      if (askResult.isAccepted()) {
        String runId = askResult.getRunId() == null ? "n/a" : askResult.getRunId();
        queryResponse = "OpenClaw accepted request (runId: " + runId + ").";
      } else if (fallbackToOllama) {
        log.warn("OpenClaw backend failed ({}), falling back to Ollama", askResult.getError());
        queryResponse = ollamaAiService.ask(
            request.getEngineRequest(),
            hostUrl,
            modelName,
            queryMessage,
            network,
            channel,
            sentByNick,
            sentByRealName
        );
      } else {
        queryResponse = "OpenClaw unavailable: " + askResult.getError();
      }
    } else {
      queryResponse = ollamaAiService.ask(
          request.getEngineRequest(),
          hostUrl,
          modelName,
          queryMessage,
          network,
          channel,
          sentByNick,
          sentByRealName
      );
    }

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


    SysEnvValue envValue = envValuesService.findFirstByKey("ollamaWaterHost");

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

          String ollamaWaterHost = envValuesService.getKeyValueOrDefault("ollamaWaterHost", "http://bot-ollama:11434");
          String ollamaWaterModel = envValuesService.getKeyValueOrDefault("ollamaWaterModel", "qwen3-vl:235b-cloud");

          String promptMessage = "What is the current measured water temperature. Answer nothing else but XXX°C  where XXX is temperature.";
          String queryResponse;
          if (envValuesService.getKeyValueBooleanOrDefault("waterUseOllama", false)) {
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
