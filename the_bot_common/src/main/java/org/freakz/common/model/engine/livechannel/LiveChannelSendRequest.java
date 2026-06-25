package org.freakz.common.model.engine.livechannel;

public record LiveChannelSendRequest(String echoToAlias, String webUsername, String message) {
}
