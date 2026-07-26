package org.freakz.engine.services.irc;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.IrcServerConfig;
import org.freakz.common.model.connectionmanager.IrcChannelControlRequest;
import org.freakz.common.model.connectionmanager.IrcChannelControlResponse;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.engine.config.ConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IrcChannelControlService {

  private final ConfigService configService;
  private final RestConnectionManagerClient connectionManagerClient;

  public IrcChannelControlService(
      ConfigService configService,
      RestConnectionManagerClient connectionManagerClient) {
    this.configService = configService;
    this.connectionManagerClient = connectionManagerClient;
  }

  public IrcChannelControlResponse control(String target, String action) {
    Channel configured = findConfiguredChannel(target);
    if (configured == null) {
      return new IrcChannelControlResponse(target, null, action, false, false,
          "IRC channel is not configured");
    }
    return connectionManagerClient.controlIrcChannel(
        new IrcChannelControlRequest(configured.getEchoToAlias(), action));
  }

  private Channel findConfiguredChannel(String target) {
    if (target == null || target.isBlank() || configService.readBotConfig().getIrcServerConfigs() == null) {
      return null;
    }
    for (IrcServerConfig server : configService.readBotConfig().getIrcServerConfigs()) {
      if (server == null || server.getChannelList() == null) {
        continue;
      }
      for (Channel channel : server.getChannelList()) {
        if (channel != null && (same(target, channel.getEchoToAlias()) || same(target, channel.getName()))) {
          return channel;
        }
      }
    }
    return null;
  }

  private boolean same(String left, String right) {
    return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
  }
}
