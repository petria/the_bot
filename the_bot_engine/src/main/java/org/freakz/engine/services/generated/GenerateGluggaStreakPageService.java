package org.freakz.engine.services.generated;

import org.freakz.common.config.TheBotProperties;
import org.freakz.common.enums.TopCountsEnum;
import org.freakz.common.generated.GeneratedPageCreated;
import org.freakz.common.generated.GeneratedPageStore;
import org.freakz.common.model.dto.DataValues;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.DataValuesService;
import org.freakz.engine.dto.generated.GeneratedPageResponse;
import org.freakz.engine.services.api.AbstractService;
import org.freakz.engine.services.api.ServiceMessageHandler;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.api.ServiceRequestType;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.GenerateGluggaStreakPage)
public class GenerateGluggaStreakPageService extends AbstractService {

  private static final Duration PAGE_TTL = Duration.ofDays(7);
  private static final String COMPONENT_TYPE = "GluggaStreakPage";

  @Override
  public void initializeService(ConfigService configService) {
  }

  @Override
  public <T extends org.freakz.engine.services.api.ServiceResponse> GeneratedPageResponse handleServiceRequest(
      ServiceRequest request) {
    EngineRequest engineRequest = request.getEngineRequest();
    String channel = GeneratedPageServiceSupport.resolveChannel(request);
    String network = GeneratedPageServiceSupport.normalize(engineRequest.getNetwork());

    DataValuesService dataValuesService = request.getApplicationContext().getBean(DataValuesService.class);
    List<DataValues> dataValues =
        dataValuesService.getRawDataValues(channel, network, TopCountsEnum.GLUGGA_COUNT.getKeyName());
    Map<String, Object> props = createProps(channel, network, dataValues);

    TheBotProperties properties = request.getApplicationContext().getBean(TheBotProperties.class);
    ConfigService configService = request.getApplicationContext().getBean(ConfigService.class);
    JsonMapper jsonMapper = request.getApplicationContext().getBean(JsonMapper.class);
    GeneratedPageStore store = new GeneratedPageStore(Path.of(configService.getRuntimeDataFileName("")), jsonMapper);

    try {
      GeneratedPageCreated created =
          store.create(COMPONENT_TYPE, String.format("Glugga streaks for %s", channel), props, PAGE_TTL);
      String publicBaseUrl =
          configService.getConfigValue(
              "the.bot.webPublicBaseUrl",
              "THE_BOT_WEB_PUBLIC_BASE_URL",
              properties.getWebPublicBaseUrl());
      String url = GeneratedPageServiceSupport.buildUrl(publicBaseUrl, created.id(), created.token());
      GeneratedPageResponse response =
          new GeneratedPageResponse(url, ((Number) props.get("nickCount")).intValue());
      response.setStatus("OK");
      return response;
    } catch (Exception e) {
      GeneratedPageResponse response = new GeneratedPageResponse();
      response.setStatus("NOK: " + e.getMessage());
      return response;
    }
  }

  private Map<String, Object> createProps(
      String channel,
      String network,
      List<DataValues> dataValues) {
    Map<String, Map<LocalDate, Integer>> nickDayCounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    int totalCount = 0;
    int skippedRows = 0;

    for (DataValues dataValue : dataValues) {
      LocalDate date = parseDate(dataValue.getKeyName());
      int count = GeneratedPageServiceSupport.parseCount(dataValue.getValue());
      if (date == null || count <= 0) {
        skippedRows++;
        continue;
      }
      nickDayCounts
          .computeIfAbsent(dataValue.getNick(), ignored -> new TreeMap<>())
          .merge(date, count, Integer::sum);
      totalCount += count;
    }

    List<Map<String, Object>> rows = createNickRows(nickDayCounts);
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("channel", channel);
    props.put("network", network);
    props.put("counterKey", TopCountsEnum.GLUGGA_COUNT.getKeyName());
    props.put("counterName", TopCountsEnum.GLUGGA_COUNT.getName());
    props.put("generatedAt", Instant.now().toString());
    props.put("totalCount", totalCount);
    props.put("rawRowCount", dataValues.size());
    props.put("skippedRowCount", skippedRows);
    props.put("nickCount", rows.size());
    props.put("rows", rows);
    return props;
  }

  private List<Map<String, Object>> createNickRows(Map<String, Map<LocalDate, Integer>> nickDayCounts) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map.Entry<String, Map<LocalDate, Integer>> entry : nickDayCounts.entrySet()) {
      Streak best = findBestStreak(entry.getValue());
      int totalCount = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("nick", entry.getKey());
      row.put("longestDays", best.length());
      row.put("startDate", best.start() == null ? null : best.start().toString());
      row.put("endDate", best.end() == null ? null : best.end().toString());
      row.put("streakCount", best.count());
      row.put("activeDays", entry.getValue().size());
      row.put("totalCount", totalCount);
      rows.add(row);
    }
    rows.sort(Comparator
        .comparingInt((Map<String, Object> row) -> ((Number) row.get("longestDays")).intValue())
        .reversed()
        .thenComparing(
            row -> row.get("endDate") == null ? "" : row.get("endDate").toString(),
            Comparator.reverseOrder())
        .thenComparing(row -> row.get("nick").toString(), String.CASE_INSENSITIVE_ORDER));
    return rows;
  }

  private Streak findBestStreak(Map<LocalDate, Integer> dayCounts) {
    Streak best = new Streak(null, null, 0, 0);
    LocalDate currentStart = null;
    LocalDate previousDate = null;
    int currentLength = 0;
    int currentCount = 0;

    for (Map.Entry<LocalDate, Integer> entry : dayCounts.entrySet()) {
      LocalDate date = entry.getKey();
      int count = entry.getValue();
      if (previousDate == null || date.equals(previousDate.plusDays(1))) {
        if (currentStart == null) {
          currentStart = date;
        }
        currentLength++;
        currentCount += count;
      } else {
        best = bestOf(best, new Streak(currentStart, previousDate, currentLength, currentCount));
        currentStart = date;
        currentLength = 1;
        currentCount = count;
      }
      previousDate = date;
    }
    if (previousDate != null) {
      best = bestOf(best, new Streak(currentStart, previousDate, currentLength, currentCount));
    }
    return best;
  }

  private Streak bestOf(Streak left, Streak right) {
    if (right.length() > left.length()) {
      return right;
    }
    if (right.length() == left.length()
        && right.end() != null
        && (left.end() == null || right.end().isAfter(left.end()))) {
      return right;
    }
    return left;
  }

  private LocalDate parseDate(String keyName) {
    try {
      String[] parts = keyName == null ? new String[0] : keyName.split("_");
      if (parts.length < 6) {
        return null;
      }
      int year = Integer.parseInt(parts[2]);
      int day = Integer.parseInt(parts[3]);
      int month = Integer.parseInt(parts[4]);
      return LocalDate.of(year, month, day);
    } catch (DateTimeException | NumberFormatException e) {
      return null;
    }
  }

  private record Streak(LocalDate start, LocalDate end, int length, int count) {
  }
}
