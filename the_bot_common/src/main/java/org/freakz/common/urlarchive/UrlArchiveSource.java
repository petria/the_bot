package org.freakz.common.urlarchive;

public record UrlArchiveSource(
    String protocol,
    String network,
    String channelAlias,
    String channelName,
    String sender) {
}
