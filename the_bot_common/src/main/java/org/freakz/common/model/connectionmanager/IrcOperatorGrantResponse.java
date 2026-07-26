package org.freakz.common.model.connectionmanager;

public record IrcOperatorGrantResponse(
    String echoToAlias,
    boolean botHasOperator,
    boolean granted,
    boolean alreadyOperator,
    String error) {
}
