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
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.GenerateGluggaWeekdayPage)
public class GenerateGluggaWeekdayPageService extends AbstractService {

  private static final Duration PAGE_TTL = Duration.ofDays(7);
  private static final String COMPONENT_TYPE = "GluggaWeekdayPage";

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
          store.create(COMPONENT_TYPE, String.format("Glugga weekday stats for %s", channel), props, PAGE_TTL);
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
    Map<DayOfWeek, Integer> dayCounts = emptyDayCounts();
    Map<String, Map<DayOfWeek, Integer>> nickDayCounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    int totalCount = 0;
    int skippedRows = 0;

    for (DataValues dataValue : dataValues) {
      DayOfWeek day = parseDayOfWeek(dataValue.getKeyName());
      int count = GeneratedPageServiceSupport.parseCount(dataValue.getValue());
      if (day == null || count <= 0) {
        skippedRows++;
        continue;
      }
      dayCounts.put(day, dayCounts.get(day) + count);
      Map<DayOfWeek, Integer> countsByDay =
          nickDayCounts.computeIfAbsent(dataValue.getNick(), ignored -> emptyDayCounts());
      countsByDay.put(day, countsByDay.get(day) + count);
      totalCount += count;
    }

    Map<String, Object> props = new LinkedHashMap<>();
    props.put("channel", channel);
    props.put("network", network);
    props.put("counterKey", TopCountsEnum.GLUGGA_COUNT.getKeyName());
    props.put("counterName", TopCountsEnum.GLUGGA_COUNT.getName());
    props.put("generatedAt", Instant.now().toString());
    props.put("totalCount", totalCount);
    props.put("rawRowCount", dataValues.size());
    props.put("skippedRowCount", skippedRows);
    props.put("nickCount", nickDayCounts.size());
    props.put("daySummary", createDaySummary(dayCounts, totalCount));
    props.put("nickRows", createNickRows(nickDayCounts));
    return props;
  }

  private Map<DayOfWeek, Integer> emptyDayCounts() {
    Map<DayOfWeek, Integer> counts = new EnumMap<>(DayOfWeek.class);
    for (DayOfWeek day : DayOfWeek.values()) {
      counts.put(day, 0);
    }
    return counts;
  }

  private List<Map<String, Object>> createDaySummary(Map<DayOfWeek, Integer> dayCounts, int totalCount) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (DayOfWeek day : DayOfWeek.values()) {
      int count = dayCounts.get(day);
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("day", dayLabel(day));
      row.put("count", count);
      row.put("percent", totalCount == 0 ? 0D : (count * 100D) / totalCount);
      rows.add(row);
    }
    return rows;
  }

  private List<Map<String, Object>> createNickRows(Map<String, Map<DayOfWeek, Integer>> nickDayCounts) {
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map.Entry<String, Map<DayOfWeek, Integer>> entry : nickDayCounts.entrySet()) {
      Map<DayOfWeek, Integer> dayCounts = entry.getValue();
      int total = dayCounts.values().stream().mapToInt(Integer::intValue).sum();
      DayOfWeek bestDay =
          dayCounts.entrySet().stream()
              .max(Comparator
                  .comparingInt((Map.Entry<DayOfWeek, Integer> item) -> item.getValue())
                  .thenComparing(item -> -item.getKey().getValue()))
              .map(Map.Entry::getKey)
              .orElse(DayOfWeek.MONDAY);
      int bestDayCount = dayCounts.get(bestDay);
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("nick", entry.getKey());
      row.put("bestDay", dayLabel(bestDay));
      row.put("bestDayCount", bestDayCount);
      row.put("bestDayPercent", total == 0 ? 0D : (bestDayCount * 100D) / total);
      row.put("totalCount", total);
      row.put("weekdayPercents", createWeekdayPercents(dayCounts, total));
      rows.add(row);
    }
    rows.sort(Comparator
        .comparingInt((Map<String, Object> row) -> ((Number) row.get("totalCount")).intValue())
        .reversed()
        .thenComparing(row -> row.get("nick").toString(), String.CASE_INSENSITIVE_ORDER));
    return rows;
  }

  private Map<String, Object> createWeekdayPercents(Map<DayOfWeek, Integer> dayCounts, int total) {
    Map<String, Object> percents = new LinkedHashMap<>();
    for (DayOfWeek day : DayOfWeek.values()) {
      int count = dayCounts.get(day);
      percents.put(dayLabel(day), total == 0 ? 0D : (count * 100D) / total);
    }
    return percents;
  }

  private DayOfWeek parseDayOfWeek(String keyName) {
    try {
      String[] parts = keyName == null ? new String[0] : keyName.split("_");
      if (parts.length < 6) {
        return null;
      }
      int year = Integer.parseInt(parts[2]);
      int day = Integer.parseInt(parts[3]);
      int month = Integer.parseInt(parts[4]);
      return LocalDate.of(year, month, day).getDayOfWeek();
    } catch (DateTimeException | NumberFormatException e) {
      return null;
    }
  }

  private String dayLabel(DayOfWeek day) {
    return switch (day) {
      case MONDAY -> "Mon";
      case TUESDAY -> "Tue";
      case WEDNESDAY -> "Wed";
      case THURSDAY -> "Thu";
      case FRIDAY -> "Fri";
      case SATURDAY -> "Sat";
      case SUNDAY -> "Sun";
    };
  }

}
