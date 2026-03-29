package org.freakz.engine.services.weather.water;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class WaterTemperatureService {

  private static final Logger log = LoggerFactory.getLogger(WaterTemperatureService.class);

  private final static String BASE_URL = "https://wwwi2.ymparisto.fi/i2/";
  private final WaterTemperatureInitializerService initializerService;
  private Map<String, WaterTemperatureData> dataMap = new HashMap<>();

  @Autowired
  public WaterTemperatureService(WaterTemperatureInitializerService initializerService) {
    this.initializerService = initializerService;
  }

  public Map<String, WaterTemperatureData> getDataMap() {
    return dataMap;
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

  //  @PostConstruct TODO
  public void scanWaterMeasurementSites() {
    initializerService.initialize(this);
  }

}