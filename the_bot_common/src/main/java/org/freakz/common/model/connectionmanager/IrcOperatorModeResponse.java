package org.freakz.common.model.connectionmanager;

import java.util.List;

public record IrcOperatorModeResponse(
    String echoToAlias,
    boolean botHasOperator,
    boolean operator,
    List<String> changed,
    List<String> unchanged,
    String error) {

  public IrcOperatorModeResponse {
    changed = changed == null ? List.of() : List.copyOf(changed);
    unchanged = unchanged == null ? List.of() : List.copyOf(unchanged);
  }
}
