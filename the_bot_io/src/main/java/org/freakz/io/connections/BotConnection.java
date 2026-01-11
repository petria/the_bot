package org.freakz.io.connections;


import org.freakz.common.model.connectionmanager.ChannelUser;
import org.freakz.common.model.feed.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BotConnection {

  private static final Logger log = LoggerFactory.getLogger(BotConnection.class);

  static int idCounter = 0;

  private int id;

  private Map<String, BotConnectionChannel> channelMap = new HashMap<>();

  private BotConnectionType type;

  public int getId() {
    return id;
  }

  public Map<String, BotConnectionChannel> getChannelMap() {
    return channelMap;
  }

  public BotConnectionType getType() {
    return type;
  }

  public BotConnection() {
    this.id = idCounter;
    idCounter++;
  }

  public BotConnection(BotConnectionType type) {
    this();
    this.type = type;
  }

  public void sendMessageTo(Message message) {
    log.error("sendMessageTo(Message message) not implemented: " + this.getClass());
  }

  public void sendRawMessage(Message message) {
    log.error("sendRawMessage(Message message) not implemented: " + this.getClass());
  }

  public String getNetwork() {
    return "ConnectionNetwork";
  }

  public List<ChannelUser> getChannelUsersByTargetAlias(
      String targetAlias, BotConnectionChannel channel) {
    //        List<String> list = List.of("getChannelUsersByTargetAlias(String targetAlias) not
    // implemented: " + this.getClass());
    //        log.error(list.get(0));
    return null;
  }
}
