package org.freakz.services;

import org.freakz.services.foreca.ForecaData;
import org.freakz.services.foreca.ForecaWeatherService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForecaWeatherServiceTest {


    @Test
    public void test_initialize_service() throws Exception {
        ForecaWeatherService sut = new ForecaWeatherService();

//        sut.initializeService();
//        List<String> byLetterLinks = sut.scanCountry("Bosnia ja Hertsegovina", "/Europe/Bosnia_and_Herzegovina/haku");


//        String byLetterLink = "/Europe/Bosnia_and_Herzegovina/haku?bl=M";
//        Map<String, String> toCollectLinks = new HashMap<>();
//        toCollectLinks = sut.scanCities("/Bosnia_and_Herzegovina", byLetterLink, toCollectLinks);

//        List<ForecaData> forecaData = sut.fetchCityWeather("Mostar", "/Bosnia_and_Herzegovina/Mostar");
        Map<String, List<String>> stringListMap = sut.collectCountryCitiLinks();

        List<String> letterLinksByCountry = stringListMap.get("Suomi");

        for (String byLetterLink : letterLinksByCountry) {
//            String byLetterLink = letterLinksByCountry.get(0);
            String countryPart = "/" + byLetterLink.split("/")[2];
            Map<String, String> toCollectLinks = new HashMap<>();
            toCollectLinks = sut.scanCities(countryPart, byLetterLink, toCollectLinks);

            Map<String, List<ForecaData>> countryAllData = new HashMap<>();
            for (String cityName : toCollectLinks.keySet()) {
                String cityUrl = toCollectLinks.get(cityName);
                if (!countryAllData.containsKey(cityName)) {
                    List<ForecaData> forecaData = sut.fetchCityWeather(cityName, cityUrl);
                    countryAllData.put(cityName, forecaData);
                }
            }
        }
        int foo = 0;


    }


}
