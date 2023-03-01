package org.freakz.services;

import org.freakz.services.foreca.ForecaWeatherService;
import org.junit.jupiter.api.Test;

public class ForecaWeatherServiceTest {


    @Test
    public void test_initialize_service() throws Exception {
        ForecaWeatherService sut = new ForecaWeatherService();

        sut.initializeService();

        int foo = 0;

//        Map<String, ForecaWeatherService.CountryCityLink> toCollectLinks = new HashMap<>();
//        ForecaWeatherService.CountryScanLinksByLetter byLetterLinks = sut.scanCountry("Bosnia ja Hertsegovina", "/Europe/Bosnia_and_Herzegovina/haku");
//        String byLetterLink = byLetterLinks.byLetterLinks.get(0);
//        toCollectLinks = sut.scanCities("/" + byLetterLinks.countryEng, byLetterLink, toCollectLinks);

//        List<ForecaData> forecaData = sut.fetchCityWeather("Mostar", "/Bosnia_and_Herzegovina/Mostar");



/*        Map<String, List<String>> stringListMap = sut.collectCountryCitiLinks();

        List<String> letterLinksByCountry = stringListMap.get("Suomi");

        for (String byLetterLink : letterLinksByCountry) {
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

*/

    }


}
