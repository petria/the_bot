package org.freakz.common.model.connectionmanager;

import java.util.List;

public record IrcOperatorModeRequest(
    String echoToAlias,
    List<String> nicks,
    boolean operator) {
}
