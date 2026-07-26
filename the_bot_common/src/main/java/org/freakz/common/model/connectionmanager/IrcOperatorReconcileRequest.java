package org.freakz.common.model.connectionmanager;

import java.util.List;

public record IrcOperatorReconcileRequest(
    String echoToAlias,
    List<String> nicks) {

  public IrcOperatorReconcileRequest {
    nicks = nicks == null ? List.of() : List.copyOf(nicks);
  }
}
