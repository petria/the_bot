package org.freakz.engine.services.water;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WaterTemperatureCommandServiceTest {
  @Test void formatsChatReplyAndPreservesIrcSenderPrefix() throws Exception {
    var index = mock(WaterPointIndexService.class);
    var readings = mock(WaterTemperatureReadingService.class);
    var engine = mock(BotEngine.class);
    var point = new WaterPointIndexService.WaterPoint("Äetsä", "station");
    when(index.resolve(anyString())).thenReturn(WaterPointIndexService.Resolution.found(point));
    when(readings.read(eq(point), any())).thenReturn(new WaterTemperatureReading("Äetsä", "station",20.5,java.time.Instant.now()));
    var service = new WaterTemperatureCommandService(index,readings,engine);
    var request = new EngineRequest(); request.setChatProtocol("irc"); request.setFromSender("nick");
    assertThat(service.lookupText(request,"Äetsä")).isEqualTo("Äetsä, 20.5°C");
    service.ask(request,"Äetsä");
    verify(engine).sendReplyMessage(request,"nick: Äetsä, 20.5°C");
  }

  @Test void acceptsNumericStringTemperatures() {
    for (String number : new String[]{"20.5","20,5"}) {
      assertThat(WaterTemperatureReadingService.parseTemperature(new JsonMapper(),
          "{\"status\":\"OK\",\"temperatureC\":\""+number+"\"}")).isEqualTo(20.5);
    }
  }

  @Test void rejectsInvalidAndUnavailableResponses() {
    for (String response : new String[]{"{\"status\":\"OK\",\"temperatureC\":\"warm\"}",
        "{\"status\":\"OK\",\"temperatureC\":99}","{\"status\":\"N/A\"}"}) {
      assertThatThrownBy(() -> WaterTemperatureReadingService.parseTemperature(new JsonMapper(),response))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
