package org.freakz.engine.services.water;

import org.freakz.common.chat.ChatIdentityUtil;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class WaterTemperatureCommandService {
  private final WaterPointIndexService index;
  private final WaterTemperatureReadingService readings;
  private final BotEngine botEngine;

  public WaterTemperatureCommandService(WaterPointIndexService index, WaterTemperatureReadingService readings, BotEngine botEngine) {
    this.index = index; this.readings = readings; this.botEngine = botEngine;
  }

  @Async
  public void ask(EngineRequest request, String location) {
    botEngine.sendReplyMessage(request, formatReplyForTarget(request, lookupText(request, location)));
  }

  public WaterTemperatureReading lookupReading(EngineRequest request, String location) throws Exception {
    var resolution = index.resolve(location == null ? "" : location.trim());
    if (!resolution.found()) throw new IllegalArgumentException("Water station not resolved");
    return readings.read(resolution.point(), request);
  }

  public WaterTemperatureReading refreshReading(EngineRequest request, String location) throws Exception {
    var resolution = index.resolve(location);
    if (!resolution.found()) throw new IllegalArgumentException("Water station not resolved");
    return readings.refresh(resolution.point(), request);
  }

  public String lookupText(EngineRequest request, String location) {
    String requested = location == null ? "" : location.trim();
    String display = requested;
    try {
      var resolution = index.resolve(requested);
      if (!resolution.found()) {
        return resolution.ambiguous() ? requested + ": N/A (matches: " + resolution.matches().stream()
            .limit(4).map(WaterPointIndexService.WaterPoint::name).reduce((a,b) -> a + ", " + b).orElse("") + ")"
            : requested + ": N/A";
      }
      display = resolution.displayName();
      var result = readings.read(resolution.point(), request);
      return String.format(Locale.ROOT, "%s, %.1f°C", display, result.temperatureC());
    } catch (Exception error) {
      LoggerFactory.getLogger(getClass()).warn("Water lookup failed for '{}': {}", requested, error.getMessage());
      return display + ": N/A";
    }
  }

  private String formatReplyForTarget(EngineRequest request, String reply) {
    if (reply == null || reply.isBlank() || request == null) {
      return reply;
    }
    String protocol = ChatIdentityUtil.sanitize(request.getChatProtocol(), ChatIdentityUtil.resolveProtocol(request.getNetwork()));
    if (!"irc".equals(protocol) || request.isPrivateChannel()) {
      return reply;
    }
    String prefix = request.getFromSender() + ": ";
    return reply.startsWith(prefix) ? reply : prefix + reply;
  }
}
