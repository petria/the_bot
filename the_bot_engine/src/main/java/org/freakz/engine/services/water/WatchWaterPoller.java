package org.freakz.engine.services.water;

import java.time.Instant;
import java.util.UUID;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.mobile.MobileNotificationEvent;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestBotWebSystemClient;
import org.freakz.engine.data.service.UsersService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

@Service
public class WatchWaterPoller {
  private final WatchWaterSettingsService settings;
  private final WaterTemperatureCommandService water;
  private final RestBotWebSystemClient mobile;
  private final UsersService users;
  private final org.freakz.engine.data.service.EnvValuesService cache;
  private final JsonMapper mapper;
  private final String station, username;
  private final long interval;
  private Instant next = Instant.EPOCH;
  private Boolean wasEnabled;
  private Double lastTemperature;
  private Instant retrievedAt;

  public WatchWaterPoller(WatchWaterSettingsService settings, WaterTemperatureCommandService water,
      RestBotWebSystemClient mobile, UsersService users, JsonMapper mapper, org.freakz.engine.data.service.EnvValuesService cache,
      @Value("${THE_BOT_WATER_WATCH_STATION:Merikoski}") String station,
      @Value("${THE_BOT_WATER_WATCH_USERNAME:}") String username,
      @Value("${THE_BOT_WATER_WATCH_INTERVAL_MINUTES:60}") long minutes) {
    this.settings=settings; this.water=water; this.mobile=mobile; this.users=users; this.mapper=mapper;
    this.cache=cache;
    this.station=station; this.username=username; this.interval=Math.max(5, minutes)*60;
  }

  @Scheduled(fixedDelay = 30000, initialDelay = 30000, scheduler = "watchWaterScheduler")
  public synchronized void poll() {
    try {
      boolean enabled = settings.getSettings().waterTemperatureToWatch();
      if (!enabled && Boolean.FALSE.equals(wasEnabled)) return;
      if (username.isBlank()) return;
      User user = users.findAll().stream().filter(User.class::isInstance).map(User.class::cast)
          .filter(u -> username.equals(u.getUsername())).findFirst().orElse(null);
      if (user == null) return;
      if (retrievedAt == null) {
        String cached = cache.getKeyValueOrDefault("water.watch.last-reading", "{}");
        var saved = mapper.readTree(cached);
        if (station.equals(saved.path("station").asString()) && username.equals(saved.path("username").asString())
            && saved.path("retrievedAt").asLong() > 0 && saved.path("temperatureC").isNumber()) {
          lastTemperature=saved.path("temperatureC").asDouble();
          retrievedAt=Instant.ofEpochMilli(saved.path("retrievedAt").asLong());
        }
      }
      Instant now = Instant.now();
      if (enabled && Boolean.TRUE.equals(wasEnabled) && now.isBefore(next)) return;
      if (enabled) {
        next=now.plusSeconds(interval);
        EngineRequest request = new EngineRequest();
        request.setUser(user); request.setNetwork("BOT_INTERNAL"); request.setFromSender(username);
        request.setCommand("water");
        String text=water.lookupText(request, station);
        var match=java.util.regex.Pattern.compile(", (-?\\d+(?:\\.\\d+)?)°C$").matcher(text);
        if (match.find()) { lastTemperature=Double.valueOf(match.group(1)); retrievedAt=now; }
      }
      long expires = retrievedAt == null ? 0 : retrievedAt.plusSeconds(interval*3).toEpochMilli();
      var body=mapper.createObjectNode();
      body.put("station", station); body.put("enabled", enabled);
      if (lastTemperature == null) body.putNull("temperatureC"); else body.put("temperatureC",lastTemperature);
      body.put("retrievedAt", retrievedAt == null ? 0 : retrievedAt.toEpochMilli());
      body.put("expiresAt", expires); body.put("updatedAt", now.toEpochMilli());
      body.put("username", username);
      cache.setEnvValue("water.watch.last-reading", mapper.writeValueAsString(body), user);
      mobile.publishMobileNotification(new MobileNotificationEvent(UUID.randomUUID().toString(),username,
          "WATER_WATCH","Water temperature",mapper.writeValueAsString(body),null,null,null,now));
      wasEnabled=enabled;
      LoggerFactory.getLogger(getClass()).info("Watch water state delivered: user={} station={} enabled={} temperatureC={} retrievedAt={}",
          username, station, enabled, lastTemperature, retrievedAt);
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).warn("Watch water poll/delivery failed: {}", e.getMessage());
    }
  }
}
