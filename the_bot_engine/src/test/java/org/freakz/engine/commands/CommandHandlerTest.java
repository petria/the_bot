package org.freakz.engine.commands;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.clients.MessageSendClient;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.KelikameratResponse;
import org.freakz.engine.dto.KelikameratUrl;
import org.freakz.engine.dto.KelikameratWeatherData;
import org.freakz.engine.functions.OpenAiService;
import org.freakz.engine.services.HokanServices;
import org.freakz.engine.services.conversations.ConversationsService;
import org.freakz.engine.services.status.CallCountInterceptor;
import org.freakz.engine.services.urls.UrlMetadataService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CommandHandlerTest {

  private MessageSendClient messageSendClient = Mockito.mock(MessageSendClient.class);

  private HokanServices hokanServices = Mockito.mock(HokanServices.class);

  private AccessService accessService = Mockito.mock(AccessService.class);

  private ConfigService configService = Mockito.mock(ConfigService.class);

  private ConversationsService conversationsService = Mockito.mock(ConversationsService.class);
  private CallCountInterceptor countInterceptor = Mockito.mock(CallCountInterceptor.class);
  private UrlMetadataService urlMetadataService = Mockito.mock(UrlMetadataService.class);
  private OpenAiService openAiService = Mockito.mock(OpenAiService.class);

  //    @Test
  public void testWeatherCmd() throws Exception {
    String command = "!weather oulu";
    when(hokanServices.doServiceRequest(any(), any())).thenReturn(getMockServiceAnswer());

    BotEngine botEngine =
        new BotEngine(
            accessService,
            messageSendClient,
            hokanServices,
            configService,
            conversationsService,
            countInterceptor,
            urlMetadataService,
            openAiService);
    /*        String reply = commandHandler.handleEngineRequest(createMockRequest(command));
    if (reply != null) {
        System.out.printf("%s: %s\n", command, reply);
    } else {
        throw new Exception(command + ": NULL reply!");
    }*/

  }

  @Test
  public void test_if_init_parameters_not_implemented_exception_is_thrown() throws Exception {
    Date date = new Date(1704468825172L);
    //                   1716742800172L
    Date date2 = new Date(1716728400172L);
    LocalDateTime localDateTime = LocalDateTime.of(2024, 5, 26, 13, 0);
    long epoc = localDateTime.toEpochSecond(ZoneOffset.of("Z"));

    String command = "!TestNoInitParams";
    when(hokanServices.doServiceRequest(any(), any())).thenReturn(getMockServiceAnswer());

    BotEngine botEngine =
        new BotEngine(
            accessService,
            messageSendClient,
            hokanServices,
            configService,
            conversationsService,
            countInterceptor,
            urlMetadataService,
            openAiService);

    NotImplementedException thrown =
        Assertions.assertThrows(
            NotImplementedException.class,
            () -> {
              String reply =
                  "TODO"; // ;commandHandler.handleEngineRequest(createMockRequest(command));
              System.out.printf("%s\n", reply);
            },
            "NumberFormatException was expected");

    Assertions.assertEquals(
        "Command handler must Override initCommandOptions(): TestNoInitParamsCmd",
        thrown.getMessage());
  }

  private Object getMockServiceAnswer() {
    KelikameratResponse response =
        KelikameratResponse.builder().dataList(getMockWeatherDataList()).build();

    response.setStatus("OK: MOCKED RESPONSE");
    return response;
  }

  private List<KelikameratWeatherData> getMockWeatherDataList() {
    List<KelikameratWeatherData> list = new ArrayList<>();

    int count = 0;
    while (count < 10) {
      LocalDateTime time = LocalDateTime.of(2020, 10, 10, 10, 10 + count);
      KelikameratUrl url =
          KelikameratUrl.builder().areaUrl("Mock area url").stationUrl("Mock station url").build();
      String place = "Place: " + count;
      String placeFromUrl = "PlaceFromUrl: " + count;

      Float air = 10.0F + (count);
      Float road = 20.0F + (count);
      Float ground = 30.0F + (count);
      Float humidity = 40.0F + (count);
      Float dewPoint = 50.0F + (count);

      list.add(
          getMockWeatherDataNode(
              time, url, place, placeFromUrl, air, road, ground, humidity, dewPoint));
      count++;
    }
    return list;
  }

  private KelikameratWeatherData getMockWeatherDataNode(
      LocalDateTime time,
      KelikameratUrl url,
      String place,
      String placeFromUrl,
      Float air,
      Float road,
      Float ground,
      Float humidity,
      Float dewPoint) {
    KelikameratWeatherData data =
        KelikameratWeatherData.builder()
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
    return EngineRequest.builder().command(line).build();
  }
}
