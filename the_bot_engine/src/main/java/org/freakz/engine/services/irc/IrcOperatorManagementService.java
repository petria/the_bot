package org.freakz.engine.services.irc;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.connectionmanager.IrcOperatorReconcileRequest;
import org.freakz.common.model.connectionmanager.IrcOperatorReconcileResponse;
import org.freakz.common.model.connectionmanager.IrcOperatorStateResponse;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.common.users.ChannelPermissionUtil;
import org.freakz.common.users.UserChatIdentityUtil;
import org.freakz.common.users.UserPermissions;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IrcOperatorManagementService {

  private final ConfigService configService;
  private final UsersService usersService;
  private final RestConnectionManagerClient connectionManagerClient;

  public IrcOperatorManagementService(
      ConfigService configService,
      UsersService usersService,
      RestConnectionManagerClient connectionManagerClient) {
    this.configService = configService;
    this.usersService = usersService;
    this.connectionManagerClient = connectionManagerClient;
  }

  public IrcOperatorReconcileResponse reconcile(String echoToAlias) {
    ChannelConfig channelConfig = findChannel(echoToAlias);
    if (channelConfig == null) {
      throw new IllegalArgumentException("IRC channel is not configured: " + echoToAlias);
    }
    if (!Boolean.TRUE.equals(channelConfig.channel().getManageOperators())) {
      throw new IllegalStateException("IRC operator management is disabled for channel: " + echoToAlias);
    }
    IrcOperatorStateResponse state = connectionManagerClient.getIrcOperatorState(echoToAlias);
    if (state == null) {
      throw new IllegalStateException("Could not read IRC operator state");
    }
    List<String> authorizedNicks = authorizedNicks(channelConfig, state.users());
    return connectionManagerClient.reconcileIrcOperators(
        new IrcOperatorReconcileRequest(echoToAlias, authorizedNicks));
  }

  public List<IrcOperatorReconcileResponse> reconcileAll() {
    List<IrcOperatorReconcileResponse> results = new ArrayList<>();
    List<IrcServerConfig> servers = configService.readBotConfig().getIrcServerConfigs();
    if (servers == null) {
      return results;
    }
    for (IrcServerConfig server : servers) {
      if (server == null) {
        continue;
      }
      for (Channel channel : server.getChannelList()) {
        if (Boolean.TRUE.equals(channel.getManageOperators())) {
          results.add(reconcile(channel.getEchoToAlias()));
        }
      }
    }
    return results;
  }

  private List<String> authorizedNicks(ChannelConfig channelConfig, List<ChannelUser> observedUsers) {
    List<String> nicks = new ArrayList<>();
    for (ChannelUser observed : observedUsers == null ? List.<ChannelUser>of() : observedUsers) {
      String nick = observed.getNick();
      if (nick == null || nick.isBlank()) {
        continue;
      }
      for (User user : usersService.findAll().stream().map(node -> (User) node).toList()) {
        if (UserPermissions.has(user, ChannelPermissionUtil.modePermission("IRC_CONNECTION", channelConfig.channel().getEchoToAlias()))
            && matchesIrcUser(user, channelConfig.network(), nick)) {
          nicks.add(nick);
          break;
        }
      }
    }
    return nicks.stream().distinct().toList();
  }

  private boolean matchesIrcUser(User user, String network, String nick) {
    if (UserChatIdentityUtil.configuredValueMatchesObserved(user.getIrcNick(), nick)) {
      return true;
    }
    return UserChatIdentityUtil.matches(user, "IRC_CONNECTION", network, null, nick, nick);
  }

  private ChannelConfig findChannel(String echoToAlias) {
    if (echoToAlias == null || configService.readBotConfig().getIrcServerConfigs() == null) {
      return null;
    }
    for (IrcServerConfig server : configService.readBotConfig().getIrcServerConfigs()) {
      if (server.getChannelList() == null) {
        continue;
      }
      for (Channel channel : server.getChannelList() == null ? List.<Channel>of() : server.getChannelList()) {
        if (channel.getEchoToAlias() != null && channel.getEchoToAlias().equalsIgnoreCase(echoToAlias)) {
          return new ChannelConfig(
              server.getIrcNetwork() == null ? null : server.getIrcNetwork().getName(),
              channel);
        }
      }
    }
    return null;
  }

  private record ChannelConfig(String network, Channel channel) {
  }
}
