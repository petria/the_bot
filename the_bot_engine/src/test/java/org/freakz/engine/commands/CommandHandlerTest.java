package org.freakz.engine.commands;

import org.freakz.clients.MessageSendClient;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.KelikameratResponse;
import org.freakz.dto.KelikameratUrl;
import org.freakz.dto.KelikameratWeatherData;
import org.freakz.services.HokanServices;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


public class CommandHandlerTest {

    private MessageSendClient messageSendClient = Mockito.mock(MessageSendClient.class);

    private HokanServices hokanServices = Mockito.mock(HokanServices.class);

    @Test
    public void testWeatherCmd() throws Exception {
        String command = "!weather oulu";


        when(hokanServices.doServiceRequest(any(), any())).thenReturn(getMockServiceAnswer());

        CommandHandler commandHandler = new CommandHandler(messageSendClient, hokanServices);
        String reply = commandHandler.handleCommand(createMockRequest(command));
        if (reply != null) {
            System.out.printf("%s: %s\n", command, reply);
        } else {
            throw new Exception(command + ": NULL reply!");
        }

    }

    private Object getMockServiceAnswer() {
        KelikameratResponse response
                = KelikameratResponse.builder()
                .dataList(getMockWeatherDataList())
                .build();

        response.setStatus("MOCKED RESPONSE");
        return response;
    }

    private List<KelikameratWeatherData> getMockWeatherDataList() {
        List<KelikameratWeatherData> list = new ArrayList<>();

        int count = 0;

        LocalDateTime time = LocalDateTime.of(2020, 10, 10, 10, 10);
        KelikameratUrl url
                = KelikameratUrl.builder()
                .areaUrl("Mock area url")
                .stationUrl("Mock station url")
                .build();
        String place = "Place: " + count;
        String placeFromUrl = "PlaceFromUrl: " + count;

        Float air = 10.0F + (count);
        Float road = 20.0F + (count);
        Float ground = 30.0F + (count);
        Float humidity = 40.0F + (count);
        Float dewPoint = 50.0F + (count);

        list.add(getMockWeatherDataNode(time, url, place, placeFromUrl, air, road, ground, humidity, dewPoint));
        return list;
    }

    private KelikameratWeatherData getMockWeatherDataNode(LocalDateTime time, KelikameratUrl url, String place, String placeFromUrl, Float air, Float road, Float ground, Float humidity, Float dewPoint) {
        KelikameratWeatherData data
                = KelikameratWeatherData.builder()
                .time(time)
                .url(url)
                .place(place)
                .placeFromUrl(placeFromUrl)
                .air(air)
                .road(road)
                .ground(ground)
                .humidity(humidity)
                .dewPoint(dewPoint)
                .build();
        return data;
    }


    private EngineRequest createMockRequest(String line) {
        return EngineRequest.builder()
                .command(line)
                .build();

    }

}
