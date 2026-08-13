package org.freakz.engine.services.irc;

import org.freakz.common.irc.IrcChannelModeSpec;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.connectionmanager.IrcModeEventRequest;
import org.freakz.common.model.connectionmanager.IrcModeEventResponse;
import org.freakz.common.model.connectionmanager.IrcModeSetRequest;
import org.freakz.common.model.connectionmanager.IrcModeSetResponse;
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
public class IrcModeManagementService {

  private final ConfigService configService;
  private final UsersService usersService;
  private final RestConnectionManagerClient connectionManagerClient;

  public IrcModeManagementService(
      ConfigService configService,
      UsersService usersService,
      RestConnectionManagerClient connectionManagerClient) {
    this.configService = configService;
    this.usersService = usersService;
    this.connectionManagerClient = connectionManagerClient;
  }

  public IrcModeEventResponse handleModeEvent(IrcModeEventRequest request) {
    ChannelConfig channelConfig = findChannel(request == null ? null : request.echoToAlias());
    if (channelConfig == null || !Boolean.TRUE.equals(channelConfig.channel().getManageMode())) {
      return new IrcModeEventResponse("IGNORE", null, false, "IRC channel mode management is disabled");
    }
    String requested = normalize(request == null ? null : request.modes());
    if (authorizedNick(request == null ? null : request.setterNick(), channelConfig)) {
      persistModes(channelConfig.channel().getEchoToAlias(), requested);
      return new IrcModeEventResponse("ACCEPT", requested, true, null);
    }
    return new IrcModeEventResponse("RESTORE", configuredModes(channelConfig.channel()), false,
        "IRC channel mode setter lacks channel admin permission");
  }

  public IrcModeSetResponse setModes(String echoToAlias, String requestedModes, EngineRequest request) {
    ChannelConfig channelConfig = findChannel(echoToAlias);
    if (channelConfig == null) {
      return new IrcModeSetResponse(echoToAlias, null, false, null, "IRC channel is not configured");
    }
    if (!Boolean.TRUE.equals(channelConfig.channel().getManageMode())) {
      return new IrcModeSetResponse(echoToAlias, channelConfig.channel().getName(), false, null,
          "IRC channel mode management is disabled");
    }
    if (request == null || !canSetMode(echoToAlias, request.getUser())) {
      return new IrcModeSetResponse(echoToAlias, channelConfig.channel().getName(), false, null, null);
    }
    String modes;
    try {
      modes = normalize(requestedModes);
    } catch (IllegalArgumentException e) {
      return new IrcModeSetResponse(echoToAlias, channelConfig.channel().getName(), false, null, e.getMessage());
    }
    persistModes(channelConfig.channel().getEchoToAlias(), modes);
    IrcModeSetResponse response = connectionManagerClient.setIrcModes(new IrcModeSetRequest(
        channelConfig.channel().getEchoToAlias(), modes));
    return response == null
        ? new IrcModeSetResponse(echoToAlias, channelConfig.channel().getName(), false, modes,
            "Could not set IRC channel modes")
        : response;
  }

  public boolean canSetMode(String echoToAlias, User user) {
    ChannelConfig channelConfig = findChannel(echoToAlias);
    if (channelConfig == null || !Boolean.TRUE.equals(channelConfig.channel().getManageMode())) {
      return false;
    }
    return UserPermissions.has(user,
        ChannelPermissionUtil.adminPermission("IRC_CONNECTION", channelConfig.channel().getEchoToAlias()));
  }

  public User findUser(String username) {
    if (username == null || username.isBlank()) {
      return null;
    }
    return usersService.findAll().stream()
        .map(node -> (User) node)
        .filter(user -> username.equalsIgnoreCase(user.getUsername()))
        .findFirst()
        .orElse(null);
  }

  public String guardedModes(String echoToAlias) {
    ChannelConfig channelConfig = findChannel(echoToAlias);
    return channelConfig == null ? null : configuredModes(channelConfig.channel());
  }

  private String normalize(String modes) {
    return IrcChannelModeSpec.parse(modes).value();
  }

  private String configuredModes(Channel channel) {
    try {
      return normalize(channel.getModes());
    } catch (IllegalArgumentException e) {
      return "";
    }
  }

  private void persistModes(String echoToAlias, String modes) {
    try {
      configService.updateIrcChannelModes(echoToAlias, modes);
    } catch (IOException e) {
      throw new IllegalStateException("Could not persist IRC channel modes", e);
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
        if (channel != null && channel.getEchoToAlias() != null
            && channel.getEchoToAlias().trim().equalsIgnoreCase(echoToAlias.trim())) {
          return new ChannelConfig(
              server.getIrcNetwork() == null ? null : server.getIrcNetwork().getName(), channel);
        }
      }
    }
    return null;
  }

  private record ChannelConfig(String network, Channel channel) {
  }
}
