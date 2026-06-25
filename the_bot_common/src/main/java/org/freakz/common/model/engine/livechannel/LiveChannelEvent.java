package org.freakz.common.model.engine.livechannel;

public record LiveChannelEvent(
    long id,
    long requestId,
    long createdAt,
    String echoToAlias,
    String sender,
    String senderId,
    String message,
    String protocol,
    String network,
    String chatType,
    String chatId,
    LiveChannelDirection direction) {
}
