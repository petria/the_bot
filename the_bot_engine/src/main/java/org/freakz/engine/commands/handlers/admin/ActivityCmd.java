package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.connectionmanager.ChannelActivityResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.ActivityResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@HokanCommandHandler
@HokanAdminCommand
public class ActivityCmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("List known channels and when the last inbound message was received there.");
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    ActivityResponse response = doServiceRequest(request, results, ServiceRequestType.ConnectionActivityService);
    List<ChannelActivityResponse> channels = response.getChannels();

    sb().append("== Activity\n");
    if (channels == null || channels.isEmpty()) {
      sb().append(" no known channels\n");
      return sb().toString();
    }

    List<ChannelActivityResponse> sorted = new ArrayList<>(channels);
    sorted.sort(Comparator.comparing(ChannelActivityResponse::getEchoToAlias, String.CASE_INSENSITIVE_ORDER));
    long now = System.currentTimeMillis();
    int aliasWidth = sorted.stream()
        .map(ChannelActivityResponse::getEchoToAlias)
        .filter(alias -> alias != null && !alias.isBlank())
        .mapToInt(String::length)
        .max()
        .orElse(10);
    int ageWidth = sorted.stream()
        .map(channel -> formatLastSeen(now, channel.getLastReceivedMessageAt()))
        .mapToInt(String::length)
        .max()
        .orElse(8);
    for (ChannelActivityResponse channel : sorted) {
      String age = formatLastSeen(now, channel.getLastReceivedMessageAt());
      format(
          " %-" + aliasWidth + "s  %-" + ageWidth + "s  %s\n",
          channel.getEchoToAlias(),
          age,
          formatActor(channel)
      );
    }
    return sb().toString();
  }

  private String formatActor(ChannelActivityResponse channel) {
    if (channel.getLastReceivedMessageAt() == null || channel.getLastReceivedMessageAt() <= 0L) {
      return "-";
    }
    String actor = channel.getLastReceivedMessageBy();
    String source = channel.getLastReceivedMessageSource();
    if ((actor == null || actor.isBlank()) && (source == null || source.isBlank())) {
      return "-";
    }
    if (actor == null || actor.isBlank()) {
      return "<" + source + ">";
    }
    if (source == null || source.isBlank()) {
      return "<" + actor + ">";
    }
    return "<" + actor + "@" + source + ">";
  }

  private String formatLastSeen(long now, Long lastReceivedMessageAt) {
    if (lastReceivedMessageAt == null || lastReceivedMessageAt <= 0L) {
      return "never";
    }
    long diffMs = Math.max(0L, now - lastReceivedMessageAt);
    long totalSeconds = diffMs / 1000L;
    long days = totalSeconds / 86400L;
    long hours = (totalSeconds % 86400L) / 3600L;
    long minutes = (totalSeconds % 3600L) / 60L;
    long seconds = totalSeconds % 60L;

    StringBuilder sb = new StringBuilder();
    if (days > 0L) {
      sb.append(days).append("d");
    }
    if (hours > 0L) {
      sb.append(hours).append("h");
    }
    if (minutes > 0L) {
      sb.append(minutes).append("m");
    }
    if (seconds > 0L || sb.isEmpty()) {
      sb.append(seconds).append("s");
    }
    sb.append(" ago");
    return sb.toString();
  }
}
