package org.freakz.common.model.engine.system;

public record HermesModelDiscoveryRequest(
    String provider,
    String baseUrl,
    String apiKey,
    String profileId) {
}
