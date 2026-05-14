package org.freakz.io.connections;


import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.feed.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BotConnection {

  private static final Logger log = LoggerFactory.getLogger(BotConnection.class);

  static int idCounter = 0;

  private int id;

  private Map<String, BotConnectionChannel> channelMap = new ConcurrentHashMap<>();

  private BotConnectionType type;

  public BotConnection() {
    this.id = idCounter;
    idCounter++;
  }

  public BotConnection(BotConnectionType type) {
    this();
    this.type = type;
  }

  public int getId() {
    return id;
  }

  public Map<String, BotConnectionChannel> getChannelMap() {
    return channelMap;
  }

  public void updateChannel(BotConnectionChannel channel) {
    if (channel == null || channel.getEchoToAlias() == null || channel.getEchoToAlias().isBlank()) {
      return;
    }
    channelMap.put(channel.getEchoToAlias().trim().toUpperCase(), channel);
  }

  public void removeChannel(String echoToAlias) {
    if (echoToAlias == null || echoToAlias.isBlank()) {
      return;
    }
    channelMap.remove(echoToAlias.trim().toUpperCase());
  }

  public void clearChannels() {
    channelMap.clear();
  }

  public BotConnectionType getType() {
    return type;
  }

  public void sendMessageTo(Message message) {
    log.error("sendMessageTo(Message message) not implemented: " + this.getClass());
  }

  public void sendRawMessage(Message message) {
    log.error("sendRawMessage(Message message) not implemented: " + this.getClass());
  }

  public void stop() {
    log.debug("stop() not implemented: {}", this.getClass());
  }

  public String getNetwork() {
    return "ConnectionNetwork";
  }

  public List<ChannelUser> getChannelUsersByEchoToAlias(
      String echoToAlias, BotConnectionChannel channel) {
    //        List<String> list = List.of("getChannelUsersByEchoToAlias(String echoToAlias) not
    // implemented: " + this.getClass());
    //        log.error(list.get(0));
    return null;
  }
}
