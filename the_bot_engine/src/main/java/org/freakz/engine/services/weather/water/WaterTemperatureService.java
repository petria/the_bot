package org.freakz.engine.services.weather.water;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WaterTemperatureService {

  private final static String BASE_URL = "https://wwwi2.ymparisto.fi/i2/";

  @Getter
  private Map<String, WaterTemperatureData> dataMap = new HashMap<>();

  private final WaterTemperatureInitializerService initializerService;

  @Autowired
  public WaterTemperatureService(WaterTemperatureInitializerService initializerService) {
    this.initializerService = initializerService;
  }

  public Map<String, String> getWaterAreaFromOptions(String html) {
    Map<String, String> forecastAreas = new HashMap<>();
    Document doc = Jsoup.parse(html);
    Element select = doc.select("select[name=forma]").first();
    if (select != null) {
      Elements options = select.select("option");
      for (Element option : options) {
        String value = option.attr("value");
        String text = option.text();
        if (!value.isEmpty()) {
          forecastAreas.put(text, value);
        }
      }
    }
    return forecastAreas;
  }

  // https://wwwi2.ymparisto.fi/i2/59/q5904450y/twlyhyt.png
  public void scanForWaterTemperatureChartImage(String areaName, String url, String basePageUrl, String subAreaPart) throws IOException {
    Document doc = Jsoup.connect(url).get();
    Element select = doc.select("img[src=twlyhyt.png]").first();
    if (select != null) {
      String alt = select.attr("alt");
      String urlForStatsPng = basePageUrl + "/" + subAreaPart + "/twlyhyt.png";

      WaterTemperatureData data = new WaterTemperatureData();
      data.setPlace1(areaName);
      data.setPlace2(alt);
      data.setWaterTemperatureChartImageUrl(urlForStatsPng);

      dataMap.put(alt, data);

    }
  }

  public void scanAreaWaterPlaces(String areaName, String areaUrl) throws IOException {

    String url = BASE_URL + areaUrl.substring(3);

    Document doc = Jsoup.connect(url).get();
    Elements select = doc.select("a");
    for (Element element : select) {
      String attr = element.attr("src");
      if (attr.contains("piste")) {

        String href = element.attr("href");
        String text = element.text();
        String urlForStatsPage = BASE_URL + href.substring(3);
        String subAreaPart = href.split("/")[2];

        scanForWaterTemperatureChartImage(areaName, urlForStatsPage, url, subAreaPart);
      }

    }


  }

  @PostConstruct
  public void scanWaterMeasurementSites() {
    initializerService.initialize(this);
  }

}