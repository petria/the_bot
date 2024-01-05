package org.freakz.engine.services.foreca;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.foreca.CountryCityLink;
import org.freakz.common.model.foreca.ForecaData;
import org.freakz.common.model.foreca.ForecaSunUpDown;
import org.freakz.common.model.foreca.ForecaWeatherData;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.CmpWeatherResponse;
import org.freakz.engine.dto.ForecaResponse;
import org.freakz.engine.services.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;


@Slf4j
@ServiceMethodHandler
public class ForecaWeatherService extends AbstractService {

    private final static ForecaDataFetcher forecaDataFetcher = new ForecaDataFetcher();

    @Override
    public void initializeService(ConfigService configService) throws Exception {
        forecaDataFetcher.initializeService(configService);
    }

    public void initWithToCollectLinks(CachedLinks links) {
        forecaDataFetcher.setCachedLink(links);

    }

    public List<CountryCityLink> getMatchingCountryCityLinks(String place) {
        log.debug("Matching cities with: {}", place);
        List<CountryCityLink> matching = new ArrayList<>();
        String[] pieces = place.toLowerCase().split("/");
        for (String cityKey : forecaDataFetcher.getToCollectLinks().keySet()) {
            CountryCityLink countryCityLink = forecaDataFetcher.getToCollectLinks().get(cityKey);
            if (countryCityLink.city.toLowerCase().matches(place) || countryCityLink.city2.toLowerCase().matches(place)) {
                matching.add(countryCityLink);
            } else {
                String region = countryCityLink.region.toLowerCase();
                String country = countryCityLink.country.toLowerCase();
                String city = countryCityLink.city.toLowerCase();
                String city2 = countryCityLink.city2.toLowerCase();
                if ((region.contains(pieces[0]) || country.contains(pieces[0])) && (city.matches(pieces[1]) || city2.matches(pieces[1]))) {
                    matching.add(countryCityLink);
                }
            }
        }
        log.debug("{} matching {} cities", place, matching.size());
        return matching;
    }


    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.CmpWeatherService)
    public <T extends ServiceResponse> CmpWeatherResponse handleCmpCmdServiceRequest(ServiceRequest request) {

        CmpWeatherResponse response
                = CmpWeatherResponse.builder()
                .build();

        if (forecaDataFetcher.getToCollectLinks() == null) {
            response.setStatus(String.format("NOK: Initial data fetch still in progress: %d / %d", 0, -1));
        } else {
            String[] places = request.getResults().getStringArray(ARG_PLACE);

            List<ForecaData> forecaDataList = new ArrayList<>();
            response.setForecaDataList(forecaDataList);

            for (String place : places) {
                place = place.toLowerCase();
                List<CountryCityLink> matching = getMatchingCountryCityLinks(place);
                for (CountryCityLink match : matching) {
                    try {
                        ForecaSunUpDown sunUpDown = ForecaSunUpDown.builder().build();
                        List<ForecaWeatherData> forecaWeatherData = forecaDataFetcher.fetchCityWeather(match.city, match.cityUrl, sunUpDown);
                        if (forecaWeatherData != null && !forecaWeatherData.isEmpty()) {
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
            }

            response.setStatus("OK: data size " + forecaDataList.size());

        }
        return response;
    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.ForecaWeatherService)
    public <T extends ServiceResponse> ForecaResponse handleForecaCmdServiceRequest(ServiceRequest request) {
        ForecaResponse response
                = ForecaResponse.builder()
                .build();

        if (forecaDataFetcher.getToCollectLinks() == null) {
            response.setStatus(String.format("NOK: Initial data fetch still in progress: %d / %d", 0, -1));
        } else {
            String place = request.getResults().getString(ARG_PLACE).toLowerCase();

            List<CountryCityLink> matching = getMatchingCountryCityLinks(place);

            List<ForecaData> forecaDataList = new ArrayList<>();
            response.setForecaDataList(forecaDataList);
            for (CountryCityLink match : matching) {
                try {
                    ForecaSunUpDown sunUpDown = ForecaSunUpDown.builder().build();
                    List<ForecaWeatherData> forecaWeatherData = forecaDataFetcher.fetchCityWeather(match.city, match.cityUrl, sunUpDown);
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
