package org.freakz.engine.services.generated;

import org.freakz.common.config.TheBotProperties;
import org.freakz.common.enums.TopCountsEnum;
import org.freakz.common.generated.GeneratedPageCreated;
import org.freakz.common.generated.GeneratedPageStore;
import org.freakz.common.model.dto.DataValuesModel;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.generated.GeneratedPageResponse;
import org.freakz.engine.services.api.AbstractService;
import org.freakz.engine.services.api.ServiceMessageHandler;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.topcounter.TopCountService;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_CHANNEL;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.GenerateGluggaCountsPage)
public class GenerateGluggaCountsPageService extends AbstractService {

  private static final Duration PAGE_TTL = Duration.ofDays(7);
  private static final String COMPONENT_TYPE = "GluggaCountsPage";

  @Override
  public void initializeService(ConfigService configService) {
  }

  @Override
  public <T extends org.freakz.engine.services.api.ServiceResponse> GeneratedPageResponse handleServiceRequest(
      ServiceRequest request) {
    EngineRequest engineRequest = request.getEngineRequest();
    String channel = resolveChannel(request);
    String network = normalize(engineRequest.getNetwork());

    TopCountService topCountService = request.getApplicationContext().getBean(TopCountService.class);
    List<DataValuesModel> dataValues =
        topCountService.getDataValuesAsc(channel, network, TopCountsEnum.GLUGGA_COUNT.getKeyName());
    Map<String, Object> props = createProps(channel, network, dataValues);

    TheBotProperties properties = request.getApplicationContext().getBean(TheBotProperties.class);
    ConfigService configService = request.getApplicationContext().getBean(ConfigService.class);
    JsonMapper jsonMapper = request.getApplicationContext().getBean(JsonMapper.class);
    GeneratedPageStore store = new GeneratedPageStore(Path.of(configService.getRuntimeDataFileName("")), jsonMapper);

    try {
      GeneratedPageCreated created =
          store.create(COMPONENT_TYPE, String.format("Glugga counts for %s", channel), props, PAGE_TTL);
      String publicBaseUrl =
          configService.getConfigValue(
              "the.bot.webPublicBaseUrl",
              "THE_BOT_WEB_PUBLIC_BASE_URL",
              properties.getWebPublicBaseUrl());
      String url = buildUrl(publicBaseUrl, created.id(), created.token());
      GeneratedPageResponse response = new GeneratedPageResponse(url, dataValues.size());
      response.setStatus("OK");
      return response;
    } catch (Exception e) {
      GeneratedPageResponse response = new GeneratedPageResponse();
      response.setStatus("NOK: " + e.getMessage());
      return response;
    }
  }

  private String resolveChannel(ServiceRequest request) {
    EngineRequest engineRequest = request.getEngineRequest();
    if (engineRequest.isPrivateChannel()) {
      return "#amigafin";
    }
    String channel = request.getResults().getString(ARG_CHANNEL, engineRequest.getReplyTo());
    return normalize(channel);
  }

  private String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  private Map<String, Object> createProps(
      String channel,
      String network,
      List<DataValuesModel> dataValues) {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("channel", channel);
    props.put("network", network);
    props.put("counterKey", TopCountsEnum.GLUGGA_COUNT.getKeyName());
    props.put("counterName", TopCountsEnum.GLUGGA_COUNT.getName());
    props.put("generatedAt", Instant.now().toString());
    props.put("rowCount", dataValues.size());

    List<Map<String, Object>> rows = new ArrayList<>();
    int rank = 1;
    for (DataValuesModel value : dataValues) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("rank", rank++);
      row.put("nick", value.getNick());
      row.put("value", parseCount(value.getValue()));
      rows.add(row);
    }
    props.put("rows", rows);
    return props;
  }

  private int parseCount(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private String buildUrl(String baseUrl, String id, String token) {
    String trimmed = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8091" : baseUrl.trim();
    if (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed + "/generated/" + id + "?token=" + token;
  }
}
