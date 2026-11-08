package org.freakz.engine.services.water;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.services.ai.commands.HermesAiCommandService;
import org.freakz.engine.services.ai.commands.ImageAnalysisToolService;
import org.junit.jupiter.api.*;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WaterTemperatureCacheTest {
  @Configuration @EnableCaching static class Caching {}
  AnnotationConfigApplicationContext context;
  final AtomicLong ticker = new AtomicLong();
  WaterPointIndexService index;
  ImageAnalysisToolService images;
  HermesAiCommandService vision;
  WaterTemperatureCommandService command;
  WaterTemperatureReadingService readings;
  final WaterPointIndexService.WaterPoint merikoski = new WaterPointIndexService.WaterPoint("Merikoski", "station/merikoski");

  @BeforeEach void setup() throws Exception {
    index = mock(WaterPointIndexService.class);
    images = mock(ImageAnalysisToolService.class);
    vision = mock(HermesAiCommandService.class);
    when(index.resolve(anyString())).thenReturn(WaterPointIndexService.Resolution.found(merikoski));
    when(index.resolveTemperatureChartUrl(any())).thenReturn("chart");
    when(images.loadFreshWaterSiteImage("chart")).thenReturn(new ImageAnalysisToolService.ImageData("image/png", "image"));
    when(vision.analyzeImage(any(), anyString(), eq("image"))).thenReturn("{\"status\":\"OK\",\"temperatureC\":4.2}");
    context = new AnnotationConfigApplicationContext();
    context.register(Caching.class);
    context.registerBean("waterTemperatureCacheManager", org.springframework.cache.caffeine.CaffeineCacheManager.class,
        () -> WaterTemperatureCacheConfig.create(ticker::get));
    context.registerBean(WaterTemperatureReadingService.class, () -> new WaterTemperatureReadingService(index, images, vision, new JsonMapper()));
    context.refresh();
    readings = context.getBean(WaterTemperatureReadingService.class);
    command = new WaterTemperatureCommandService(index, readings, mock(BotEngine.class));
  }
  @AfterEach void close() { context.close(); }

  @Test void chatAndWatchShareResultAcrossAliasesAndCallers() throws Exception {
    assertThat(command.lookupText(new EngineRequest(), "merikoski")).isEqualTo("Merikoski, 4.2°C");
    var first = command.lookupReading(new EngineRequest(), " MERIKOSKI ");
    var second = command.lookupReading(new EngineRequest(), "alias");
    assertThat(first).isSameAs(second);
    verify(vision, times(1)).analyzeImage(any(), anyString(), anyString());
    verify(images, times(1)).loadFreshWaterSiteImage("chart");
  }

  @Test void forcedRefreshBypassesAndReplacesCachedReading() throws Exception {
    var original = readings.read(merikoski, new EngineRequest());
    when(vision.analyzeImage(any(), anyString(), anyString())).thenReturn("{\"status\":\"OK\",\"temperatureC\":5.1}");
    var refreshed = command.refreshReading(new EngineRequest(), "Merikoski");
    assertThat(refreshed.temperatureC()).isEqualTo(5.1);
    assertThat(refreshed).isNotSameAs(original);
    assertThat(readings.read(merikoski, new EngineRequest())).isSameAs(refreshed);
    verify(vision, times(2)).analyzeImage(any(), anyString(), anyString());
  }

  @Test void failedForcedRefreshPreservesCachedReading() throws Exception {
    var original = readings.read(merikoski, new EngineRequest());
    when(vision.analyzeImage(any(), anyString(), anyString())).thenThrow(new IllegalStateException("offline"));
    assertThatThrownBy(() -> command.refreshReading(new EngineRequest(), "Merikoski")).isInstanceOf(IllegalStateException.class);
    assertThat(readings.read(merikoski, new EngineRequest())).isSameAs(original);
  }

  @Test void expiresAfterSixtyMinutesAndHitsDoNotExtendLifetime() throws Exception {
    var first = readings.read(merikoski, new EngineRequest());
    ticker.set(Duration.ofMinutes(59).toNanos());
    assertThat(readings.read(merikoski, new EngineRequest())).isSameAs(first);
    ticker.set(Duration.ofMinutes(60).toNanos());
    assertThat(readings.read(merikoski, new EngineRequest())).isNotSameAs(first);
    verify(vision, times(2)).analyzeImage(any(), anyString(), anyString());
    verify(images, times(2)).loadFreshWaterSiteImage("chart");
  }

  @Test void differentStationsHaveIndependentEntriesAndZeroIsValid() throws Exception {
    when(vision.analyzeImage(any(), anyString(), anyString())).thenReturn("{\"status\":\"OK\",\"temperatureC\":0}");
    assertThat(readings.read(merikoski, new EngineRequest()).temperatureC()).isZero();
    var other = new WaterPointIndexService.WaterPoint("Other", "station/other");
    readings.read(other, new EngineRequest()); readings.read(merikoski, new EngineRequest());
    verify(vision, times(2)).analyzeImage(any(), anyString(), anyString());
  }

  @Test void invalidResultsAndExceptionsAreNotCached() throws Exception {
    when(vision.analyzeImage(any(), anyString(), anyString()))
        .thenReturn("{\"status\":\"N/A\"}", "{\"status\":\"OK\",\"temperatureC\":99}")
        .thenThrow(new IllegalStateException("offline"))
        .thenReturn("{\"status\":\"OK\",\"temperatureC\":4.2}");
    for (int i=0; i<3; i++) assertThat(command.lookupText(new EngineRequest(), "merikoski")).isEqualTo("Merikoski: N/A");
    assertThat(command.lookupText(new EngineRequest(), "merikoski")).isEqualTo("Merikoski, 4.2°C");
    command.lookupReading(new EngineRequest(), "merikoski");
    verify(vision, times(4)).analyzeImage(any(), anyString(), anyString());
  }

  @Test void concurrentRequestsForOneStationOnlyRunVisionOnce() throws Exception {
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    when(vision.analyzeImage(any(), anyString(), anyString())).thenAnswer(inv -> {
      entered.countDown();
      if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test timeout");
      return "{\"status\":\"OK\",\"temperatureC\":4.2}";
    });
    try (var pool = Executors.newFixedThreadPool(6)) {
      var futures = new ArrayList<Future<WaterTemperatureReading>>();
      for (int i=0;i<6;i++) futures.add(pool.submit(() -> command.lookupReading(new EngineRequest(), "merikoski")));
      try { assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue(); } finally { release.countDown(); }
      var first = futures.getFirst().get(5, TimeUnit.SECONDS);
      for (var future : futures) assertThat(future.get(5, TimeUnit.SECONDS)).isSameAs(first);
    }
    verify(vision, times(1)).analyzeImage(any(), anyString(), anyString());
  }
}
