package org.freakz.services.kelikamerat;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.util.StringStuff;
import org.freakz.dto.KelikameratResponse;
import org.freakz.dto.KelikameratUrl;
import org.freakz.dto.KelikameratWeatherData;
import org.freakz.services.ServiceMessageHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceRequestHandler;
import org.freakz.services.ServiceRequestType;
import org.freakz.services.ServiceResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Petri Airio on 08.02.2023.
 */
@Service
@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.KelikameratService)
public class KeliKameratService implements ServiceRequestHandler {

    private static final String BASE_ULR = "https://www.kelikamerat.info";

    private static final String[] KELIKAMERAT_URLS = {
            "https://www.kelikamerat.info/kelikamerat/Etel%C3%A4-Karjala",
            "https://www.kelikamerat.info/kelikamerat/Etel%C3%A4-Pohjanmaa",
            "https://www.kelikamerat.info/kelikamerat/Etel%C3%A4-Savo",
            "https://www.kelikamerat.info/kelikamerat/Kainuu",
            "https://www.kelikamerat.info/kelikamerat/Kanta-H%C3%A4me",
            "https://www.kelikamerat.info/kelikamerat/Keski-Pohjanmaa",
            "https://www.kelikamerat.info/kelikamerat/Keski-Suomi",
            "https://www.kelikamerat.info/kelikamerat/Kymenlaakso",
            "https://www.kelikamerat.info/kelikamerat/Lappi",

            "https://www.kelikamerat.info/kelikamerat/P%C3%A4ij%C3%A4t-H%C3%A4me",
            "https://www.kelikamerat.info/kelikamerat/Pirkanmaa",
            "https://www.kelikamerat.info/kelikamerat/Pohjanmaa",
            "https://www.kelikamerat.info/kelikamerat/Pohjois-Karjala",
            "https://www.kelikamerat.info/kelikamerat/Pohjois-Pohjanmaa",
            "https://www.kelikamerat.info/kelikamerat/Pohjois-Savo",
            "https://www.kelikamerat.info/kelikamerat/Satakunta",
            "https://www.kelikamerat.info/kelikamerat/Uusimaa",
            "https://www.kelikamerat.info/kelikamerat/Varsinais-Suomi"
    };

    private List<KelikameratUrl> stationUrls;

    private List<KelikameratWeatherData> weatherDataList;

    public void updateStations() throws IOException {
        List<KelikameratUrl> stations = new ArrayList<>();
        for (String url : KELIKAMERAT_URLS) {
            Document doc = Jsoup.connect(url).get();
            Elements elements = doc.getElementsByClass("road-camera");
//            this.dataFetched += doc.html().length();
//            this.itemsFetched++;
            for (int xx = 0; xx < elements.size(); xx++) {
                Element div = elements.get(xx);
                Element href = div.child(0);
                String hrefUrl = BASE_ULR + href.attributes().get("href");
                KelikameratUrl kelikameratUrl = new KelikameratUrl(url, hrefUrl);
                stations.add(kelikameratUrl);
            }
        }
        this.stationUrls = stations;
    }

    private Float parseFloat(String str) {
        String f = str.split(" ")[0];
        if (f != null) {
            //&& f.length() > 0 && !f.equals("-"))
            if (f.equals("-")) {
                return null;
            } else {
                return Float.parseFloat(f);
            }
        } else {
            return null;
        }
    }

    public KelikameratWeatherData updateKelikameratWeatherData(KelikameratUrl url) {
        Document doc;
        try {
            doc = Jsoup.connect(url.getStationUrl()).get();
//            this.dataFetched += doc.html().length();
//            this.itemsFetched++;
        } catch (IOException e) {
            //            log.error("Can't update data: {}", url.getStationUrl());
            return null;
        }
        String titleText = doc.getElementsByTag("title").get(0).text();

        titleText = titleText.replaceFirst("Kelikamerat - ", "").replaceFirst("\\| Kelikamerat", "").trim();

        Elements elements = doc.getElementsByClass("weather-details");
        Element div = elements.get(0);
        Element table = div.child(0);
        Element tbody = table.child(0);

        KelikameratWeatherData data = new KelikameratWeatherData();
        data.setPlace(titleText);
//        log.debug("place: {}", titleText);

        data.setUrl(url);

        int idx = url.getStationUrl().lastIndexOf("/");
        data.setPlaceFromUrl(StringStuff.htmlEntitiesToText(url.getStationUrl().substring(idx + 1)));

        String air = tbody.child(0).child(1).text();
        Float airFloat = parseFloat(air);
        if (airFloat == null) {
            return null;
        }
        data.setAir(airFloat);

        String road = tbody.child(1).child(1).text();
        data.setRoad(parseFloat(road));

        String ground = tbody.child(2).child(1).text();
        data.setGround(parseFloat(ground));
        //      log.debug("air: {} - road: {} - ground: {}", air);

        String humidity = tbody.child(3).child(1).text();
        data.setHumidity(parseFloat(humidity));

        String dewPoint = tbody.child(4).child(1).text();
        data.setDewPoint(parseFloat(dewPoint));

        Elements elements2 = doc.getElementsByClass("date-time");
        if (elements2.size() > 0) {
            String timestamp = elements2.get(0).text().substring(12);

            String pattern1 = "dd.MM.yyyy HH:mm:ss";
            String pattern2 = "dd.MM.yyyy H:mm:ss";
            //            DateTime dateTime = DateTime.parse(timestamp, DateTimeFormat.forPattern(pattern));

            LocalDateTime localDateTime;

            try {
                localDateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern(pattern1));
            } catch (DateTimeParseException exception) {
                localDateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern(pattern2));
            }

            data.setTime(localDateTime);
        }

        return data;
    }

    public List<KelikameratUrl> getStationUrls() {
        return stationUrls;
    }

    //    @Override
    public void doUpdateData() throws Exception {
        if (this.stationUrls == null) {
            updateStations();
        }
        List<KelikameratWeatherData> weatherDataList = new ArrayList<>();
        int success = 0;
        int failed = 0;
        for (KelikameratUrl url : this.stationUrls) {
            //            log.debug("Handle url: {}", url);
            KelikameratWeatherData data = updateKelikameratWeatherData(url);
            if (data != null) {
                weatherDataList.add(data);
                success++;
            } else {
                failed++;
            }
            //      log.debug("{}", String.format("%s: %1.2f °C", data.getPlaceFromUrl(), data.getAir()));
        }

        log.debug("Update done, success: {} / failed: {}", success, failed);
//        Collections.sort(weatherDataList);
//        this.itemCount = weatherDataList.size();
        this.weatherDataList = weatherDataList;
    }

    @Override
    public ServiceResponse<?> handleServiceRequest(ServiceRequest request) {
        KelikameratResponse response = new KelikameratResponse();
        ServiceResponse<KelikameratResponse> r = new ServiceResponse<>(response);
        return r;
    }



/*    @ServiceMessageHandler(ServiceRequestType = ServiceRequestType.WEATHER_REQUEST_NEW)
    public void handleWeatherRequest(ServiceRequest request, ServiceResponse response) {
        String originalText = (String) request.getParameters()[0];
        KelikameratResponse response1 = new KelikameratResponse();
        response.setResponseData(request.getType().getResponseDataKey(), response1);
    }
*/
}
