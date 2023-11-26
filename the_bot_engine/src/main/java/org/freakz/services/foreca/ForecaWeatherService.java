package org.freakz.services.foreca;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.foreca.*;
import org.freakz.config.ConfigService;
import org.freakz.dto.ForecaResponse;
import org.freakz.services.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.ForecaWeatherService)
public class ForecaWeatherService extends AbstractService {


    private Map<String, CountryCityLink> toCollectLinks = null;
    private ObjectMapper mapper = new ObjectMapper();

    String urlBase = "https://www.foreca.fi";

    public String[] REGION_URLS
            = {
            "https://www.foreca.fi/Asia/haku",
            "https://www.foreca.fi/Africa/haku",
            "https://www.foreca.fi/Antarctica/haku",
            "https://www.foreca.fi/Australia_and_Oceania/haku",
            "https://www.foreca.fi/Europe/haku",
            "https://www.foreca.fi/South_America/haku",
            "https://www.foreca.fi/North_America/haku",
            "https://www.foreca.fi/North_America/United_States/haku"
    };

    public void initWithToCollectLinks(CachedLinks links) {
        toCollectLinks = links.getToCollectLinks();
    }

    public void initializeService(ConfigService configService) throws Exception {
        String countryMatch = ".*";


        File dataFile = configService.getRuntimeDataFile("foreca_data_cache.json");

        if (dataFile.exists()) {
            log.debug("Reading cached data file: {}", dataFile.getAbsoluteFile());
            CachedLinks cachedLinks = mapper.readValue(dataFile, CachedLinks.class);
            toCollectLinks = cachedLinks.getToCollectLinks();
            log.debug("Reading cached data file DONE, city count: {}", toCollectLinks.size());
            return;
        } else {
            log.warn("Cache file not exists: {}", dataFile.getName());
        }


        Map<String, CountryCityLink> toCollectLinksMap = new HashMap<>();
        for (String regionUrl : REGION_URLS) {

            log.debug("Scanning region: {}", regionUrl);
            Map<String, CountryScanLinksByLetter> links = collectCountryCityLinks(regionUrl);

            for (String country : links.keySet()) {
                if (country.matches(countryMatch)) {
                    CountryScanLinksByLetter byLetterLinks = links.get(country);
                    for (String byLetterLink : byLetterLinks.byLetterLinks) {
                        toCollectLinksMap = scanCities("/" + byLetterLinks.countryEng, byLetterLink, toCollectLinksMap);

                    }
                }
            }
        }

        toCollectLinks = toCollectLinksMap;
        log.debug("City map ready, city count: {}", toCollectLinks.size());

        log.debug("Writing data to json start: {}", dataFile.getName());
        CachedLinks cachedLinks = new CachedLinks(toCollectLinks);
        mapper.writeValue(dataFile, cachedLinks);
        log.debug("Write done!");

    }


    public Map<String, CountryScanLinksByLetter> collectCountryCityLinks(String regionUrl) throws Exception {
//        String url = "https://www.foreca.fi/Europe/haku";

        String region = regionUrl.split("/")[3];

        Map<String, CountryScanLinksByLetter> byCountryLinks = new HashMap<>();
        Document doc = Jsoup.connect(regionUrl).get();
        Elements a = doc.getElementsByTag("a");
        for (Element element : a) {
            String href1 = element.attributes().get("href");
            if (href1.startsWith("/" + region + "/") && href1.endsWith("/haku")) {
//            if (href1.startsWith("/Europe/") && href1.endsWith("/haku")) {
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
        Document doc;
        try {
            doc = Jsoup.connect(url).get();
        } catch (MalformedURLException ex) {
            log.error("Can't scan url: {}", url);
            return toCollectLinks;
        }

        Elements a = doc.getElementsByTag("a");
        String[] split = byLetterLink.split("/");

        String region = split[1];
        String country = split[2];
        for (Element element : a) {
            String href1 = element.attributes().get("href");
            if (href1.startsWith(baseLink)) {
                String city = element.text();
//                log.debug("city: {}", city);
                CountryCityLink data = new CountryCityLink();
                data.region = region;
                data.country = country;
                data.city = city;
                String[] hrefSplit = href1.split("/");
                data.city2 = hrefSplit[hrefSplit.length - 1];
                data.cityUrl = String.format("%s/%s", baseLink, city);
                toCollectLinks.put(region + country + city, data);
            }
        }
        return toCollectLinks;
    }

    public List<ForecaWeatherData> fetchCityWeather(String cityName, String cityUrl, ForecaSunUpDown sunUpDown) throws Exception {

        String url = urlBase + cityUrl;
        log.debug("fetch city data: {} -> {}", cityName, cityUrl);
        Document doc = Jsoup.connect(url).get();
        Elements scripts = doc.getElementsByTag("script");
        for (Element script : scripts) {

            if (script.childNodeSize() > 0) {
                Node node = script.childNode(0);
                String s = node.toString();
                if (s.startsWith("fcainit.push")) {
                    List<ForecaWeatherData> forecaWeatherData = extractWeatherData(s, "var observations = {", "};");
                    ForecaSunUpDown upDown = extractSunRiseData(s, "var sun = {", "};");
                    sunUpDown.setDayLengthTotalMinutes(upDown.getDayLengthTotalMinutes());
                    sunUpDown.setDayLengthHours(upDown.getDayLengthHours());
                    sunUpDown.setDayLengthMinutes(upDown.getDayLengthMinutes());
                    sunUpDown.setSunUpTime(upDown.getSunUpTime());
                    sunUpDown.setSunDownTime(upDown.getSunDownTime());
                    return forecaWeatherData;
                }

            }
        }

        return null;
    }

    private ForecaSunUpDown extractSunRiseData(String from, String start, String end) {
        String sun = extractPart(from, start, end);
        if (engine != null) {
            String script = String.format("%s\n sun", sun);
            try {
                Object eval = engine.eval(script);
                Map values = (Map) eval;
                try {
                    ForecaSunUpDown sunUpDown
                            = ForecaSunUpDown.builder()
                            .dayLengthTotalMinutes(getAsInteger(values.get("daylen")))
                            .dayLengthHours(getAsInteger(values.get("daylen_h")))
                            .dayLengthMinutes(getAsInteger(values.get("daylen_m")))
                            .sunUpTime((String) values.get("sunup"))
                            .sunDownTime((String) values.get("sundown"))
                            .build();

                    return sunUpDown;

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (ScriptException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    private static ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");

    private String extractPart(String from, String start, String end) {
        String part = null;
        int idx1 = from.indexOf(start);
        if (idx1 != -1) {
            int idx2 = from.indexOf(end, idx1);
            if (idx2 != -1) {
                part = from.substring(idx1, idx2 + 2);
            }
        }
        return part;
    }


    private List<ForecaWeatherData> extractWeatherData(String from, String start, String end) {
        List<ForecaWeatherData> forecaData = new ArrayList<>();
        String observations = extractPart(from, start, end);
        if (engine != null) {
            String script = String.format("%s\n observations", observations);
            try {
                Object eval = engine.eval(script);
                Map dataMap = (Map) eval;
                for (Object object : dataMap.keySet()) {
                    Map values = (Map) dataMap.get(object);
                    Integer key = Integer.parseInt((String) object);
                    try {
                        ForecaWeatherData foreca = ForecaWeatherData.builder()
                                .key(key)
                                .date((String) values.get("date"))
                                .time((String) values.get("time"))
                                .temp(getAsDouble(values.get("temp")))
                                .feelsLike(getAsDouble(values.get("flike")))
                                .relativeHumidity(getAsDouble(values.get("rhum")))
                                .visibility(getAsDouble(values.get("vis")))
                                .visibilityUnit((String) values.get("visunit"))
                                .pressure(getAsDouble(values.get("pressure")))
                                .build();
                        forecaData.add(foreca);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                forecaData.sort((o1, o2) -> o1.getKey().compareTo(o2.getKey()));
            } catch (ScriptException e) {
                throw new RuntimeException(e);
            }
        }
        return forecaData;
    }

    private Integer getAsInteger(Object temp) {
        if (temp == null) {
            return null;
        }
        String str = "" + temp;
        return Integer.valueOf(str);
    }

    private Double getAsDouble(Object temp) {
        if (temp == null) {
            return Double.NaN;
        }
        String str = "" + temp;
        return Double.valueOf(str);
    }

    public List<CountryCityLink> getMatchingCountryCityLinks(String place) {
        log.debug("Matching cities with: {}", place);
        List<CountryCityLink> matching = new ArrayList<>();
        for (String cityKey : toCollectLinks.keySet()) {
            CountryCityLink countryCityLink = toCollectLinks.get(cityKey);
            if (countryCityLink.city.toLowerCase().matches(place) || countryCityLink.city2.toLowerCase().matches(place)) {
                matching.add(countryCityLink);
            }
        }
        log.debug("{} matching {} cities", place, matching.size());
        return matching;
    }

    @Override
    public <T extends ServiceResponse> ForecaResponse handleServiceRequest(ServiceRequest request) {
        ForecaResponse response
                = ForecaResponse.builder()
                .build();

        if (toCollectLinks == null) {
            response.setStatus(String.format("NOK: Initial data fetch still in progress: %d / %d", 0, -1));
        } else {
            String place = request.getResults().getString(ARG_PLACE).toLowerCase();

            List<CountryCityLink> matching = getMatchingCountryCityLinks(place);

            List<ForecaData> forecaDataList = new ArrayList<>();
            response.setForecaDataList(forecaDataList);
            for (CountryCityLink match : matching) {
                try {
                    ForecaSunUpDown sunUpDown = ForecaSunUpDown.builder().build();
                    List<ForecaWeatherData> forecaWeatherData = fetchCityWeather(match.city, match.cityUrl, sunUpDown);
                    if (forecaWeatherData != null && forecaWeatherData.size() > 0) {
                        ForecaData forecaData
                                = ForecaData.builder()
                                .cityLink(match)
                                .weatherData(forecaWeatherData.get(0))
                                .sunUpDown(sunUpDown)
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
