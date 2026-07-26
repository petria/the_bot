package org.freakz.common.model.connectionmanager;

import java.util.List;

public record IrcOperatorStateResponse(
    String echoToAlias,
    String channelName,
    boolean botHasOperator,
    List<ChannelUser> users) {

  public IrcOperatorStateResponse {
    users = users == null ? List.of() : List.copyOf(users);
  }
}
