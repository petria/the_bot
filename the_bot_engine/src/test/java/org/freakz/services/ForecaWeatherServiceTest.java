package org.freakz.services;

import org.freakz.common.model.json.foreca.ForecaSunUpDown;
import org.freakz.common.model.json.foreca.ForecaWeatherData;
import org.freakz.services.foreca.ForecaWeatherService;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ForecaWeatherServiceTest {


    @Test
    public void test_initialize_service() throws Exception {
        ForecaWeatherService sut = new ForecaWeatherService();

//        sut.initializeService();


//        Map<String, CountryCityLink> toCollectLinks = new HashMap<>();
//https://www.foreca.fi/
//        CountryScanLinksByLetter byLetterLinks = sut.scanCountry("Yhdysvallat", "/North_America/United_States/haku");
//        String byLetterLink = byLetterLinks.byLetterLinks.get(11);
//        toCollectLinks = sut.scanCities("/" + byLetterLinks.countryEng, byLetterLink, toCollectLinks);

        ForecaSunUpDown sunUpDown = ForecaSunUpDown.builder().build();
        List<ForecaWeatherData> forecaData = sut.fetchCityWeather("Mostar", "/Bosnia_and_Herzegovina/Mostar", sunUpDown);

        int foo = 0;


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
