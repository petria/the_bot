package org.freakz.engine.services.generated;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.config.TheBotProperties;
import org.freakz.common.generated.GeneratedPageStore;
import org.freakz.common.model.dto.DataValuesModel;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.generated.GeneratedPageResponse;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.topcounter.TopCountService;
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

class GenerateGluggaCountsPageServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void generatesSecretLinkWithAllGluggaCountRows() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    ApplicationContext context = mock(ApplicationContext.class);
    TopCountService topCountService = mock(TopCountService.class);
    ConfigService configService = mock(ConfigService.class);
    TheBotProperties properties = new TheBotProperties();
    properties.setWebPublicBaseUrl("http://fallback");

    when(context.getBean(TopCountService.class)).thenReturn(topCountService);
    when(context.getBean(TheBotProperties.class)).thenReturn(properties);
    when(context.getBean(ConfigService.class)).thenReturn(configService);
    when(context.getBean(JsonMapper.class)).thenReturn(mapper);
    when(configService.getRuntimeDataFileName("")).thenReturn(tempDir.toString() + "/");
    when(configService.getConfigValue(
        eq("the.bot.webPublicBaseUrl"),
        eq("THE_BOT_WEB_PUBLIC_BASE_URL"),
        eq("http://fallback"))).thenReturn("http://bot-web.local:8091");
    when(topCountService.getDataValuesAsc("#hokandev", "ircnet", "GLUGGA_COUNT"))
        .thenReturn(List.of(
            new DataValuesModel("NickA", "#hokandev", "ircnet", "GLUGGA_COUNT", "12"),
            new DataValuesModel("NickB", "#hokandev", "ircnet", "GLUGGA_COUNT", "7")));

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

    GeneratedPageResponse response = new GenerateGluggaCountsPageService().handleServiceRequest(request);

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

    assertThat(props.get("channel")).isEqualTo("#hokandev");
    assertThat(props.get("network")).isEqualTo("ircnet");
    assertThat(props.get("rowCount")).isEqualTo(2);
    assertThat((List<?>) props.get("rows")).hasSize(2);
  }

  private com.martiansoftware.jsap.JSAPResult results(String args) throws Exception {
    JSAP jsap = new JSAP();
    jsap.registerParameter(new UnflaggedOption(ARG_CHANNEL).setRequired(false).setGreedy(false));
    return jsap.parse(args);
  }
}
