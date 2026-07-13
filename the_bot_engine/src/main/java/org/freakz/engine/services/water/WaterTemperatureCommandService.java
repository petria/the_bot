package org.freakz.engine.services.water;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.ai.commands.HermesAiCommandService;
import org.freakz.engine.services.ai.commands.ImageAnalysisToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;

@Service
public class WaterTemperatureCommandService {

  private static final Logger log = LoggerFactory.getLogger(WaterTemperatureCommandService.class);
  private static final String VISION_PROMPT = """
      Read the current measured river water temperature from the attached SYKE chart.
      Use only the observed blue temperature line. Ignore the cyan dashed forecast-start line,
      forecast bands, labels, and forecast values. Read the rightmost observed blue point before
      the forecast marker. Return JSON only, exactly one of:
      {"status":"OK","temperatureC":20.5}
      {"status":"N/A"}
      temperatureC must be a JSON number in degrees Celsius, with no unit text.
      """;

  private final WaterPointIndexService pointIndexService;
  private final ImageAnalysisToolService imageAnalysisToolService;
  private final HermesAiCommandService hermesAiCommandService;
  private final JsonMapper jsonMapper;
  private final BotEngine botEngine;

  public WaterTemperatureCommandService(
      WaterPointIndexService pointIndexService,
      ImageAnalysisToolService imageAnalysisToolService,
      HermesAiCommandService hermesAiCommandService,
      JsonMapper jsonMapper,
      BotEngine botEngine) {
    this.pointIndexService = pointIndexService;
    this.imageAnalysisToolService = imageAnalysisToolService;
    this.hermesAiCommandService = hermesAiCommandService;
    this.jsonMapper = jsonMapper;
    this.botEngine = botEngine;
  }

  @Async
  public void ask(EngineRequest request, String requestedLocation) {
    String requested = requestedLocation == null ? "" : requestedLocation.trim();
    String response;
    try {
      WaterPointIndexService.Resolution resolution = pointIndexService.resolve(requested);
      if (!resolution.found()) {
        response = resolution.ambiguous()
            ? requested + ": N/A (matches: " + resolution.matches().stream()
                .limit(4).map(WaterPointIndexService.WaterPoint::name).reduce((a, b) -> a + ", " + b).orElse("") + ")"
            : requested + ": N/A";
      } else {
        String chartUrl = pointIndexService.resolveTemperatureChartUrl(resolution.point());
        if (chartUrl == null || chartUrl.isBlank()) {
          response = resolution.displayName() + ": N/A";
        } else {
          ImageAnalysisToolService.ImageData image = imageAnalysisToolService.loadWaterSiteImage(chartUrl);
          String modelResponse = hermesAiCommandService.analyzeImage(request, VISION_PROMPT, image.dataUrl());
          response = formatTemperature(resolution.displayName(), modelResponse);
        }
      }
    } catch (Exception error) {
      log.warn("Water temperature lookup failed for '{}': {}", requested, error.getMessage());
      response = requested + ": N/A";
    }
    botEngine.sendReplyMessage(request, formatReplyForTarget(request, response));
  }

  String formatTemperature(String pointName, String modelResponse) {
    try {
      String json = modelResponse == null ? "" : modelResponse.trim();
      int start = json.indexOf('{');
      int end = json.lastIndexOf('}');
      if (start >= 0 && end > start) {
        json = json.substring(start, end + 1);
      }
      JsonNode node = jsonMapper.readTree(json);
      if (!"OK".equalsIgnoreCase(node.path("status").asString("")) || !node.path("temperatureC").isNumber()) {
        return pointName + ": N/A";
      }
      double temperature = node.path("temperatureC").asDouble(Double.NaN);
      if (!Double.isFinite(temperature) || temperature < -10 || temperature > 45) {
        return pointName + ": N/A";
      }
      return String.format(Locale.ROOT, "%s, %.1f°C", pointName, temperature);
    } catch (Exception error) {
      return pointName + ": N/A";
    }
  }

  private String formatReplyForTarget(EngineRequest request, String reply) {
    if (reply == null || reply.isBlank() || request == null) {
      return reply;
    }
    String protocol = ChatIdentityUtil.sanitize(request.getChatProtocol(), ChatIdentityUtil.resolveProtocol(request.getNetwork()));
    if (!"irc".equals(protocol) || request.isPrivateChannel()) {
      return reply;
    }
    String prefix = request.getFromSender() + ": ";
    return reply.startsWith(prefix) ? reply : prefix + reply;
  }
}
