package org.freakz.engine.services.weather.water;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class WaterTemperatureInitializerService {

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
