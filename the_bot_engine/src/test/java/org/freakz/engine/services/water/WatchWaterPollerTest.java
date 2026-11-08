package org.freakz.engine.services.water;

import org.freakz.common.model.engine.system.SystemSettings;
import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestBotWebSystemClient;
import org.freakz.engine.data.service.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import java.util.List;

class WatchWaterPollerTest {
  @Test void pollsOnceThenHonorsIntervalAndSendsDisabledState() throws Exception {
    var settings=mock(WatchWaterSettingsService.class);
    var water=mock(WaterTemperatureCommandService.class);
    var mobile=mock(RestBotWebSystemClient.class);
    var users=mock(UsersService.class);
    var cache=mock(EnvValuesService.class);
    var mapper=JsonMapper.builder().build();
    when(settings.getSettings()).thenReturn(new SystemSettings(true));
    doReturn(List.of(User.builder().username("petria").name("Petri").build())).when(users).findAll();
    when(cache.getKeyValueOrDefault(anyString(),anyString())).thenReturn("{}");
    when(water.lookupText(any(),eq("Merikoski"))).thenReturn("Merikoski, 4.2°C");
    var poller=new WatchWaterPoller(settings,water,mobile,users,mapper,cache,"Merikoski","petria",30);
    poller.poll(); poller.poll();
    verify(water,times(1)).lookupText(any(),eq("Merikoski"));
    var event=ArgumentCaptor.forClass(MobileNotificationEvent.class);
    verify(mobile).publishMobileNotification(event.capture());
    var data=mapper.readTree(event.getValue().body());
    assertThat(data.path("temperatureC").asDouble()).isEqualTo(4.2);
    assertThat(data.path("expiresAt").asLong()-data.path("retrievedAt").asLong()).isEqualTo(90*60*1000);
    assertThat(event.getValue().username()).isEqualTo("petria");
    when(settings.getSettings()).thenReturn(new SystemSettings(false));
    poller.poll(); poller.poll();
    verify(mobile,times(2)).publishMobileNotification(event.capture());
    assertThat(mapper.readTree(event.getValue().body()).path("enabled").asBoolean()).isFalse();
  }

  @Test void failedLookupKeepsCachedReadingAndItsOriginalExpiry() throws Exception {
    var settings=mock(WatchWaterSettingsService.class);
    var water=mock(WaterTemperatureCommandService.class);
    var mobile=mock(RestBotWebSystemClient.class);
    var users=mock(UsersService.class);
    var cache=mock(EnvValuesService.class);
    when(settings.getSettings()).thenReturn(new SystemSettings(true));
    doReturn(List.of(User.builder().username("petria").build())).when(users).findAll();
    long retrieved=System.currentTimeMillis()-60000;
    when(cache.getKeyValueOrDefault(anyString(),anyString())).thenReturn("{\"station\":\"Merikoski\",\"username\":\"petria\",\"temperatureC\":0.0,\"retrievedAt\":"+retrieved+"}");
    when(water.lookupText(any(),anyString())).thenReturn("Merikoski: N/A");
    new WatchWaterPoller(settings,water,mobile,users,JsonMapper.builder().build(),cache,"Merikoski","petria",30).poll();
    var event=ArgumentCaptor.forClass(MobileNotificationEvent.class);
    verify(mobile).publishMobileNotification(event.capture());
    var data=JsonMapper.builder().build().readTree(event.getValue().body());
    assertThat(data.path("temperatureC").asDouble()).isEqualTo(0);
    assertThat(data.path("retrievedAt").asLong()).isEqualTo(retrieved);
  }
}
