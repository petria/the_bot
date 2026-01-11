package org.freakz.engine.services.weather.water;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class WaterTemperatureInitializerService {

  private static final Logger log = LoggerFactory.getLogger(WaterTemperatureInitializerService.class);

  @Async
  public void initialize(WaterTemperatureService service) {
    try {
      String url = "https://wwwi2.ymparisto.fi/i2/95/vesiA.html";
      Document doc = Jsoup.connect(url).get();

      Map<String, String> forecastAreas = service.getWaterAreaFromOptions(doc.html());

      if (forecastAreas != null && !forecastAreas.isEmpty()) {
        log.debug("Start scanning water forecast places: {}", forecastAreas.size());
        service.getDataMap().clear();
        for (Map.Entry<String, String> entry : forecastAreas.entrySet()) {
          service.scanAreaWaterPlaces(entry.getKey(), entry.getValue());
        }
        log.debug("End scanning water forecast places, total places: {}", service.getDataMap().size());
      }

    } catch (IOException e) {
      log.error("Failed to initialize WaterTemperatureService", e);
    }
  }

}
