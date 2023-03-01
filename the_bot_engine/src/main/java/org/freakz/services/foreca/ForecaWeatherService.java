package org.freakz.services.foreca;

import lombok.extern.slf4j.Slf4j;
import org.freakz.services.AbstractService;
import org.freakz.services.ServiceMessageHandler;
import org.freakz.services.ServiceRequestType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.ForecaWeatherService)
public class ForecaWeatherService extends AbstractService {
    String urlBase = "https://www.foreca.fi";

    public void initializeService() throws Exception {
        Map<String, List<String>> links = collectCountryCitiLinks();
    }

    public Map<String, List<String>> collectCountryCitiLinks() throws Exception {
        String url = "https://www.foreca.fi/Europe/haku";
        Map<String, List<String>> byCountryLinks = new HashMap<>();
        Document doc = Jsoup.connect(url).get();
        Elements a = doc.getElementsByTag("a");
        for (Element element : a) {
            String href1 = element.attributes().get("href");
            if (href1.startsWith("/Europe/") && href1.endsWith("/haku")) {
                String country = element.text();
                int foo = 0;
                List<String> list = scanCountry(country, href1);
                byCountryLinks.put(country, list);
            }
        }
        return byCountryLinks;
    }

    public List<String> scanCountry(String country, String href) throws Exception {
        log.debug("Scan for country: {} -> {}", country, href);
        String url = urlBase + href;
        Document doc = Jsoup.connect(url).get();
        Elements active = doc.getElementsByClass("active");
        String activeLetter = active.text();
        //      log.debug("Active letter: {}", activeLetter);
        List<String> byLetterLinks = new ArrayList<>();
        byLetterLinks.add(String.format("%s?bl=%s", href, activeLetter));

        Elements a = doc.getElementsByTag("a");
        for (Element element : a) {
            String href1 = element.attributes().get("href");
            if (href1.contains("haku?bl=")) {
                String letter = element.text();
//                log.debug("letter: {}", letter);
                byLetterLinks.add(String.format("%s?bl=%s", href, letter));
            }
        }
        return byLetterLinks;
    }

    public Map<String, String> scanCities(String baseLink, String byLetterLink, Map<String, String> toCollectLinks) throws Exception {
        String url = urlBase + byLetterLink;
        log.debug("scanCities: {}", url);
        Document doc = Jsoup.connect(url).get();
        Elements a = doc.getElementsByTag("a");
        //      List<String> byCityLinks = new ArrayList<>();
//        Map<String, String> cityNameToCityLinkMap = new HashMap<>();
        for (Element element : a) {
            String href1 = element.attributes().get("href");
            if (href1.startsWith(baseLink)) {
                String city = element.text();
                log.debug("city: {}", city);
                toCollectLinks.put(city, String.format("%s/%s", baseLink, city));
//                byCityLinks.add(String.format("%s/%s", baseLink, city));
            }
        }
        return toCollectLinks;
    }

    public List<ForecaData> fetchCityWeather(String cityName, String cityUrl) throws Exception {

        String url = urlBase + cityUrl;
        log.debug("fetch city data: {} -> {}", cityName, cityUrl);
        Document doc = Jsoup.connect(url).get();
        Elements scripts = doc.getElementsByTag("script");
        for (Element script : scripts) {

            if (script.childNodeSize() > 0) {
                Node node = script.childNode(0);
                String s = node.toString();
                if (s.startsWith("fcainit.push")) {
                    return extractWeatherData(s);
                }

            }
        }

        return null;
    }

    private static ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");

    private List<ForecaData> extractWeatherData(String s) {
        List<ForecaData> forecaData = new ArrayList<>();
        int idx1 = s.indexOf("var observations = {");
        if (idx1 != -1) {
            int idx2 = s.indexOf("};", idx1);
            if (idx2 != -1) {
                String data = s.substring(idx1, idx2 + 2);

                if (engine != null) {
                    String script = String.format("%s\n observations", data);
                    try {
                        Object eval = engine.eval(script);
//                        return e
                        Map dataMap = (Map) eval;
                        for (Object object : dataMap.keySet()) {
                            Map values = (Map) dataMap.get(object);
                            try {
                                ForecaData foreca
                                        = ForecaData.builder()
                                        .date((String) values.get("date"))
                                        .time((String) values.get("time"))
                                        .temp(getTemp(values.get("temp")))
                                        .feelsLike(getTemp(values.get("flike")))
                                        .relativeHumidity(getTemp(values.get("rhum")))
                                        .visibility(getTemp(values.get("vis")))
                                        .visibilityUnit((String) values.get("visunit"))
                                        .pressure(getTemp(values.get("pressure")))
                                        .build();
                                forecaData.add(foreca);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        int fuu = 3;
                    } catch (ScriptException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }


        int foo = 0;
        return forecaData;
    }

    private Double getTemp(Object temp) {
        if (temp == null) {
            return Double.NaN;
        }
        String str = "" + temp;
        return Double.valueOf(str);
    }
}
