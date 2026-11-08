package org.freakz.engine.services.water;

import java.time.Instant;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.services.ai.commands.HermesAiCommandService;
import org.freakz.engine.services.ai.commands.ImageAnalysisToolService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class WaterTemperatureReadingService {
  private static final String VISION_PROMPT = """
      Read the current measured river water temperature from the attached SYKE chart.
      Use only the observed blue temperature line. Ignore the cyan dashed forecast-start line,
      forecast bands, labels, and forecast values. Read the rightmost observed blue point before
      the forecast marker. Return JSON only, exactly one of:
      {"status":"OK","temperatureC":20.5}
      {"status":"N/A"}
      temperatureC must be a JSON number in degrees Celsius, with no unit text.
      """;

  private final WaterPointIndexService index;
  private final ImageAnalysisToolService images;
  private final HermesAiCommandService vision;
  private final JsonMapper mapper;

  public WaterTemperatureReadingService(WaterPointIndexService index, ImageAnalysisToolService images,
      HermesAiCommandService vision, JsonMapper mapper) {
    this.index = index; this.images = images; this.vision = vision; this.mapper = mapper;
  }

  @Cacheable(cacheNames = "waterTemperatures", cacheManager = "waterTemperatureCacheManager",
      key = "#point.pageUrl()", sync = true)
  public WaterTemperatureReading read(WaterPointIndexService.WaterPoint point, EngineRequest request) throws Exception {
    return fetch(point, request);
  }

  @org.springframework.cache.annotation.CachePut(cacheNames = "waterTemperatures", cacheManager = "waterTemperatureCacheManager", key = "#point.pageUrl()")
  public WaterTemperatureReading refresh(WaterPointIndexService.WaterPoint point, EngineRequest request) throws Exception {
    return fetch(point, request);
  }

  private WaterTemperatureReading fetch(WaterPointIndexService.WaterPoint point, EngineRequest request) throws Exception {
    String url = index.resolveTemperatureChartUrl(point);
    if (url == null || url.isBlank()) throw new IllegalStateException("Water chart unavailable");
    var image = images.loadFreshWaterSiteImage(url);
    String response = vision.analyzeImage(request, VISION_PROMPT, image.dataUrl());
    double value = parseTemperature(mapper, response);
    var reading = new WaterTemperatureReading(point.name(), point.pageUrl(), value, Instant.now());
    org.slf4j.LoggerFactory.getLogger(getClass()).debug("Processed fresh water reading: station={} temperatureC={} retrievedAt={}",
        reading.station(), value, reading.retrievedAt());
    return reading;
  }

  static double parseTemperature(JsonMapper mapper, String response) {
    String json = response == null ? "" : response.trim();
    int start = json.indexOf('{'), end = json.lastIndexOf('}');
    if (start >= 0 && end > start) json = json.substring(start, end + 1);
    JsonNode node = mapper.readTree(json);
    Double value = readTemperature(node.path("temperatureC"));
    if (!"OK".equalsIgnoreCase(node.path("status").asString("")) || value == null || value < -10 || value > 45)
      throw new IllegalArgumentException("Water temperature unavailable or invalid");
    return value;
  }

  private static Double readTemperature(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    if (node.isNumber()) {
      double value = node.asDouble(Double.NaN);
      return Double.isFinite(value) ? value : null;
    }
    if (node.isString()) {
      String text = node.asString("").trim().replace(',', '.');
      try {
        double value = Double.parseDouble(text);
        return Double.isFinite(value) ? value : null;
      } catch (NumberFormatException error) {
        return null;
      }
    }
    return null;
  }

}
