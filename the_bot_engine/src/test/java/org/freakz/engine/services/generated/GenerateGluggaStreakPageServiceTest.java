package org.freakz.engine.services.generated;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.config.TheBotProperties;
import org.freakz.common.generated.GeneratedPageStore;
import org.freakz.common.model.dto.DataValues;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.DataValuesService;
import org.freakz.engine.dto.generated.GeneratedPageResponse;
import org.freakz.engine.services.api.ServiceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_CHANNEL;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenerateGluggaStreakPageServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void generatesLongestConsecutiveDayStreaksPerNick() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    ApplicationContext context = mock(ApplicationContext.class);
    DataValuesService dataValuesService = mock(DataValuesService.class);
    ConfigService configService = mock(ConfigService.class);
    TheBotProperties properties = new TheBotProperties();
    properties.setWebPublicBaseUrl("http://fallback");

    when(context.getBean(DataValuesService.class)).thenReturn(dataValuesService);
    when(context.getBean(TheBotProperties.class)).thenReturn(properties);
    when(context.getBean(ConfigService.class)).thenReturn(configService);
    when(context.getBean(JsonMapper.class)).thenReturn(mapper);
    when(configService.getRuntimeDataFileName("")).thenReturn(tempDir.toString() + "/");
    when(configService.getConfigValue(
        eq("the.bot.webPublicBaseUrl"),
        eq("THE_BOT_WEB_PUBLIC_BASE_URL"),
        eq("http://fallback"))).thenReturn("http://bot-web.local:8091");
    when(dataValuesService.getRawDataValues("#hokandev", "ircnet", "GLUGGA_COUNT"))
        .thenReturn(List.of(
            new DataValues("NickA", "ircnet", "#hokandev", "GLUGGA_COUNT_2026_01_05_10", "2"),
            new DataValues("NickA", "ircnet", "#hokandev", "GLUGGA_COUNT_2026_02_05_10", "3"),
            new DataValues("NickA", "ircnet", "#hokandev", "GLUGGA_COUNT_2026_03_05_10", "4"),
            new DataValues("NickA", "ircnet", "#hokandev", "GLUGGA_COUNT_2026_05_05_10", "9"),
            new DataValues("NickB", "ircnet", "#hokandev", "GLUGGA_COUNT_2026_10_05_10", "1"),
            new DataValues("NickB", "ircnet", "#hokandev", "GLUGGA_COUNT_2026_11_05_10", "1"),
            new DataValues("NickB", "ircnet", "#hokandev", "GLUGGA_COUNT_bad", "5")));

    ServiceRequest request =
        ServiceRequest.builder()
            .applicationContext(context)
            .engineRequest(EngineRequest.builder()
                .replyTo("#HokanDEV")
                .network("IRCNet")
                .isPrivateChannel(false)
                .build())
            .results(results(""))
            .build();

    GeneratedPageResponse response = new GenerateGluggaStreakPageService().handleServiceRequest(request);

    assertThat(response.getStatus()).isEqualTo("OK");
    assertThat(response.getRowCount()).isEqualTo(2);
    assertThat(response.getUrl()).startsWith("http://bot-web.local:8091/generated/");

    URI uri = URI.create(response.getUrl());
    String id = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
    String token = uri.getQuery().substring("token=".length());
    Map<String, Object> props =
        new GeneratedPageStore(tempDir, mapper)
            .readPublic(id, token)
            .orElseThrow()
            .getProps();

    assertThat(props.get("totalCount")).isEqualTo(20);
    assertThat(props.get("skippedRowCount")).isEqualTo(1);
    assertThat((List<?>) props.get("rows"))
        .first()
        .satisfies(row -> {
          Map<?, ?> item = (Map<?, ?>) row;
          assertThat(item.get("nick")).isEqualTo("NickA");
          assertThat(item.get("longestDays")).isEqualTo(3);
          assertThat(item.get("startDate")).isEqualTo("2026-05-01");
          assertThat(item.get("endDate")).isEqualTo("2026-05-03");
          assertThat(item.get("streakCount")).isEqualTo(9);
          assertThat(item.get("activeDays")).isEqualTo(4);
          assertThat(item.get("totalCount")).isEqualTo(18);
        });
  }

  private com.martiansoftware.jsap.JSAPResult results(String args) throws Exception {
    JSAP jsap = new JSAP();
    jsap.registerParameter(new UnflaggedOption(ARG_CHANNEL).setRequired(false).setGreedy(false));
    return jsap.parse(args);
  }
}
