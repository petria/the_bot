package org.freakz.common.model.connectionmanager;

import java.util.List;

public record IrcOperatorReconcileResponse(
    String echoToAlias,
    boolean botHasOperator,
    List<String> granted,
    List<String> skipped,
    String error) {

  public IrcOperatorReconcileResponse {
    granted = granted == null ? List.of() : List.copyOf(granted);
    skipped = skipped == null ? List.of() : List.copyOf(skipped);
  }
}
