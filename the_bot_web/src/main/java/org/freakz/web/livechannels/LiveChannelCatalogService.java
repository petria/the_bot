package org.freakz.web.livechannels;

import org.freakz.common.model.connectionmanager.BotConnectionChannelResponse;
import org.freakz.common.model.connectionmanager.BotConnectionResponse;
import org.freakz.common.model.connectionmanager.ChannelActivityResponse;
import org.freakz.common.model.connectionmanager.GetChannelActivityResponse;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LiveChannelCatalogService {

  private final RestConnectionManagerClient connectionManagerClient;

  public LiveChannelCatalogService(RestConnectionManagerClient connectionManagerClient) {
    this.connectionManagerClient = connectionManagerClient;
  }

  public List<LiveChannelCatalogItem> publicChannels() {
    GetConnectionMapResponse connectionMapResponse = connectionManagerClient.getConnectionMapRequired();
    GetChannelActivityResponse activityResponse = connectionManagerClient.getChannelActivityRequired();
    Map<String, LiveChannelCatalogItem> channels = new LinkedHashMap<>();

    if (connectionMapResponse != null && connectionMapResponse.getConnectionMap() != null) {
      for (BotConnectionResponse connection : connectionMapResponse.getConnectionMap().values()) {
        if (connection == null || connection.getChannels() == null) {
          continue;
        }
        for (BotConnectionChannelResponse channel : connection.getChannels()) {
          LiveChannelCatalogItem item = fromConnectionChannel(channel, connection.getType(), connection.getNetwork());
          if (isPublicChannel(item)) {
            channels.putIfAbsent(item.echoToAlias(), item);
          }
        }
      }
    }

    if (activityResponse != null && activityResponse.getChannels() != null) {
      for (ChannelActivityResponse activity : activityResponse.getChannels()) {
        LiveChannelCatalogItem item = fromActivity(activity);
        if (isPublicChannel(item)) {
          channels.putIfAbsent(item.echoToAlias(), item);
        }
      }
    }

    return channels.values().stream()
        .sorted(Comparator.comparing(LiveChannelCatalogItem::label, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private LiveChannelCatalogItem fromConnectionChannel(
      BotConnectionChannelResponse channel,
      String connectionType,
      String connectionNetwork) {
    if (channel == null) {
      return new LiveChannelCatalogItem(null, null, null, null, null);
    }
    return new LiveChannelCatalogItem(
        channel.getEchoToAlias(),
        label(connectionType, connectionNetwork, channel.getName(), channel.getId(), channel.getEchoToAlias()),
        connectionType,
        connectionNetwork,
        channel.getType());
  }

  private LiveChannelCatalogItem fromActivity(ChannelActivityResponse activity) {
    if (activity == null) {
      return new LiveChannelCatalogItem(null, null, null, null, null);
    }
    return new LiveChannelCatalogItem(
        activity.getEchoToAlias(),
        label(activity.getType(), activity.getNetwork(), activity.getName(), activity.getEchoToAlias(), activity.getEchoToAlias()),
        activity.getType(),
        activity.getNetwork(),
        activity.getName());
  }

  private boolean isPublicChannel(LiveChannelCatalogItem item) {
    if (item == null || item.echoToAlias() == null || item.echoToAlias().isBlank()) {
      return false;
    }
    String alias = item.echoToAlias();
    String type = item.channelType();
    return !alias.startsWith("PRIVATE-")
        && (type == null || !type.toLowerCase().contains("private"));
  }

  private String label(String connectionType, String network, String name, String id, String echoToAlias) {
    List<String> parts = new ArrayList<>();
    parts.add(blankTo(connectionType, "UNKNOWN"));
    parts.add(blankTo(network, "unknown"));
    parts.add(blankTo(firstNonBlank(name, id, echoToAlias), "-"));
    parts.add(blankTo(echoToAlias, "-"));
    return String.join(" / ", parts);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  public record LiveChannelCatalogItem(
      String echoToAlias,
      String label,
      String connectionType,
      String network,
      String channelType) {
  }
}
