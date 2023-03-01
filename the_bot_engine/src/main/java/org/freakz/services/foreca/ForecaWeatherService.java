package org.freakz.services.foreca;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.foreca.CountryCityLink;
import org.freakz.common.model.json.foreca.CountryScanLinksByLetter;
import org.freakz.common.model.json.foreca.ForecaData;
import org.freakz.common.model.json.foreca.ForecaWeatherData;
import org.freakz.dto.ForecaResponse;
import org.freakz.services.AbstractService;
import org.freakz.services.ServiceMessageHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceRequestType;
import org.freakz.services.ServiceResponse;
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

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.ForecaWeatherService)
public class ForecaWeatherService extends AbstractService {

    private static Map<String, CountryCityLink> toCollectLinks = null;

    String urlBase = "https://www.foreca.fi";


    public void initializeService() throws Exception {
        String countryMatch = "Suomi";

        Map<String, CountryCityLink> toCollectLinks = new HashMap<>();
        Map<String, CountryScanLinksByLetter> links = collectCountryCityLinks();

        for (String country : links.keySet()) {
            if (country.matches(countryMatch)) {
                CountryScanLinksByLetter byLetterLinks = links.get(country);
                for (String byLetterLink : byLetterLinks.byLetterLinks) {
                    toCollectLinks = scanCities("/" + byLetterLinks.countryEng, byLetterLink, toCollectLinks);

                }
            }
        }
        this.toCollectLinks = toCollectLinks;
        log.debug("City map ready, size: {}", this.toCollectLinks.size());

    }

    public Map<String, CountryScanLinksByLetter> collectCountryCityLinks() throws Exception {
        String url = "https://www.foreca.fi/Europe/haku";
        Map<String, CountryScanLinksByLetter> byCountryLinks = new HashMap<>();
        Document doc = Jsoup.connect(url).get();
        Elements a = doc.getElementsByTag("a");
        for (Element element : a) {
            String href1 = element.attributes().get("href");
            if (href1.startsWith("/Europe/") && href1.endsWith("/haku")) {
                String country = element.text();
                CountryScanLinksByLetter countryScanLinksByLetter = scanCountry(country, href1);
                byCountryLinks.put(country, countryScanLinksByLetter);
            }
        }
        return byCountryLinks;
    }


    public CountryScanLinksByLetter scanCountry(String country, String href) throws Exception {
        log.debug("Scan for country: {} -> {}", country, href);
        String url = urlBase + href;
        Document doc = Jsoup.connect(url).get();
        Elements active = doc.getElementsByClass("active");
        String activeLetter = active.text();
        //      log.debug("Active letter: {}", activeLetter);

        CountryScanLinksByLetter data = new CountryScanLinksByLetter();
        data.countryFin = country;
        data.countryEng = href.split("/")[2];
        List<String> byLetterLinks = data.byLetterLinks;

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
        return data;
    }


    public Map<String, CountryCityLink> scanCities(String baseLink, String byLetterLink, Map<String, CountryCityLink> toCollectLinks) throws Exception {
        String url = urlBase + byLetterLink;
        log.debug("scanCities: {}", url);
        Document doc = Jsoup.connect(url).get();
        Elements a = doc.getElementsByTag("a");
        String[] split = byLetterLink.split("/");

        String region = split[1];
        String country = split[2];
        for (Element element : a) {
            String href1 = element.attributes().get("href");
            if (href1.startsWith(baseLink)) {
                String city = element.text();
                log.debug("city: {}", city);
                CountryCityLink data = new CountryCityLink();
                data.region = region;
                data.country = country;
                data.city = city;
                data.cityUrl = String.format("%s/%s", baseLink, city);
                toCollectLinks.put(city, data);
//                byCityLinks.add(String.format("%s/%s", baseLink, city));
            }
        }
        return toCollectLinks;
    }

    public List<ForecaWeatherData> fetchCityWeather(String cityName, String cityUrl) throws Exception {

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

    private List<ForecaWeatherData> extractWeatherData(String s) {
        List<ForecaWeatherData> forecaData = new ArrayList<>();
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
                                ForecaWeatherData foreca
                                        = ForecaWeatherData.builder()
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


    @Override
    public <T extends ServiceResponse> ForecaResponse handleServiceRequest(ServiceRequest request) {
        ForecaResponse response
                = ForecaResponse.builder()
                .build();

        if (toCollectLinks == null) {
            response.setStatus(String.format("NOK: Initial data fetch still in progress: %d / %d", 0, -1));
        } else {
            String place = request.getResults().getString(ARG_PLACE);
            List<CountryCityLink> matching = new ArrayList<>();
            for (String cityKey : toCollectLinks.keySet()) {
                if (cityKey.toLowerCase().matches(place)) {
                    matching.add(toCollectLinks.get(cityKey));
                }
            }
            log.debug("{} matching {} cities", place, matching.size());
            List<ForecaData> forecaDataList = new ArrayList<>();
            response.setForecaDataList(forecaDataList);
            for (CountryCityLink match : matching) {
                try {
                    List<ForecaWeatherData> forecaWeatherData = fetchCityWeather(match.city, match.cityUrl);
                    if (forecaWeatherData != null && forecaWeatherData.size() > 0) {
                        ForecaData forecaData
                                = ForecaData.builder()
                                .cityLink(match)
                                .weatherData(forecaWeatherData.get(0))
                                .build();
                        forecaDataList.add(forecaData);
                    }
                } catch (Exception e) {
                    response.setStatus("NOK: Foreca data fetch error: " + e.getMessage());
                    break;
                }
            }
            response.setStatus("OK: data size " + forecaDataList.size());

        }

        return response;
    }

}
