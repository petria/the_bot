package org.freakz.common.media;

public record MediaStoreSource(
    String protocol,
    String network,
    String channelAlias,
    String channelName,
    String sender) {
}
