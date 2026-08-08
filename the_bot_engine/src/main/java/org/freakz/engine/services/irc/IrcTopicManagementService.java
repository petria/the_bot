package org.freakz.engine.services.irc;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.connectionmanager.IrcTopicEventRequest;
import org.freakz.common.model.connectionmanager.IrcTopicEventResponse;
import org.freakz.common.model.connectionmanager.IrcTopicSetRequest;
import org.freakz.common.model.connectionmanager.IrcTopicSetResponse;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.common.users.ChannelPermissionUtil;
import org.freakz.common.users.UserChatIdentityUtil;
import org.freakz.common.users.UserPermissions;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class IrcTopicManagementService {

  static final int FALLBACK_TOPIC_LIMIT = 390;

  private final ConfigService configService;
  private final UsersService usersService;
  private final RestConnectionManagerClient connectionManagerClient;

  public IrcTopicManagementService(
      ConfigService configService,
      UsersService usersService,
      RestConnectionManagerClient connectionManagerClient) {
    this.configService = configService;
    this.usersService = usersService;
    this.connectionManagerClient = connectionManagerClient;
  }

  public IrcTopicEventResponse handleTopicEvent(IrcTopicEventRequest request) {
    ChannelConfig channelConfig = findChannel(request == null ? null : request.echoToAlias());
    if (channelConfig == null || !Boolean.TRUE.equals(channelConfig.channel().getManageTopic())) {
      return new IrcTopicEventResponse("IGNORE", null, false, "IRC topic management is disabled");
    }

    String configuredTopic = clean(channelConfig.channel().getTopic());
    String requestedTopic = truncate(request == null ? null : request.topic());
    if (authorizedNick(request == null ? null : request.setterNick(), channelConfig)) {
      persistTopic(channelConfig.channel().getEchoToAlias(), requestedTopic);
      return new IrcTopicEventResponse(
          "ACCEPT",
          requestedTopic,
          true,
          truncationMessage(request == null ? null : request.topic(), requestedTopic));
    }
    return new IrcTopicEventResponse("RESTORE", configuredTopic == null ? "" : configuredTopic, false,
        "IRC topic setter lacks channel admin permission");
  }

  public IrcTopicSetResponse setTopic(String echoToAlias, String requestedTopic, EngineRequest request) {
    ChannelConfig channelConfig = findChannel(echoToAlias);
    if (channelConfig == null) {
      return new IrcTopicSetResponse(echoToAlias, null, false, false, null, "IRC channel is not configured");
    }
    if (!Boolean.TRUE.equals(channelConfig.channel().getManageTopic())) {
      return new IrcTopicSetResponse(echoToAlias, channelConfig.channel().getName(), false, false, null,
          "IRC topic management is disabled");
    }
    String required = ChannelPermissionUtil.adminPermission("IRC_CONNECTION", channelConfig.channel().getEchoToAlias());
    if (request == null || !UserPermissions.has(request.getUser(), required)) {
      return new IrcTopicSetResponse(echoToAlias, channelConfig.channel().getName(), false, false, null, null);
    }
    String topic = truncate(requestedTopic);
    persistTopic(channelConfig.channel().getEchoToAlias(), topic);
    IrcTopicSetResponse response = connectionManagerClient.setIrcTopic(new IrcTopicSetRequest(
        channelConfig.channel().getEchoToAlias(), topic));
    if (response == null) {
      return new IrcTopicSetResponse(echoToAlias, channelConfig.channel().getName(), false,
          !equals(topic, requestedTopic), topic, "Could not set IRC topic");
    }
    return new IrcTopicSetResponse(response.echoToAlias(), response.channelName(), response.changed(),
        response.truncated() || !equals(topic, requestedTopic), topic, response.error());
  }

  public String guardedTopic(String echoToAlias) {
    ChannelConfig channelConfig = findChannel(echoToAlias);
    return channelConfig == null ? null : clean(channelConfig.channel().getTopic());
  }

  private void persistTopic(String echoToAlias, String topic) {
    try {
      configService.updateIrcChannelTopic(echoToAlias, topic == null ? "" : topic);
    } catch (IOException e) {
      throw new IllegalStateException("Could not persist IRC topic", e);
    }
  }

  private boolean authorizedNick(String nick, ChannelConfig channelConfig) {
    if (nick == null || nick.isBlank()) {
      return false;
    }
    String permission = ChannelPermissionUtil.adminPermission("IRC_CONNECTION", channelConfig.channel().getEchoToAlias());
    for (User user : usersService.findAll().stream().map(node -> (User) node).toList()) {
      if (UserPermissions.has(user, permission)
          && (UserChatIdentityUtil.configuredValueMatchesObserved(user.getIrcNick(), nick)
          || UserChatIdentityUtil.matches(user, "IRC_CONNECTION", channelConfig.network(), nick, nick, nick))) {
        return true;
      }
    }
    return false;
  }

  private ChannelConfig findChannel(String echoToAlias) {
    if (echoToAlias == null || echoToAlias.isBlank()) {
      return null;
    }
    List<IrcServerConfig> servers = configService.readBotConfig().getIrcServerConfigs();
    if (servers == null) {
      return null;
    }
    for (IrcServerConfig server : servers) {
      for (Channel channel : server.getChannelList() == null ? List.<Channel>of() : server.getChannelList()) {
        if (channel != null && equals(channel.getEchoToAlias(), echoToAlias)) {
          return new ChannelConfig(
              server.getIrcNetwork() == null ? null : server.getIrcNetwork().getName(), channel);
        }
      }
    }
    return null;
  }

  private String truncate(String value) {
    String cleaned = value == null ? "" : value;
    return cleaned.length() <= FALLBACK_TOPIC_LIMIT ? cleaned : cleaned.substring(0, FALLBACK_TOPIC_LIMIT);
  }

  private String truncationMessage(String original, String truncated) {
    return original != null && !original.equals(truncated)
        ? "Topic was truncated to IRC limit" : null;
  }

  private boolean equals(String left, String right) {
    return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private record ChannelConfig(String network, Channel channel) {
  }
}
