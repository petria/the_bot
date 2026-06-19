package org.freakz.common.model.engine.system;

public record HermesFallbackProfileStatus(
    String profileId,
    String expectedRoute,
    boolean healthy,
    boolean openAiAvailable,
    String cooldownUntil,
    String detail,
    String activeProvider,
    String fallbackReason,
    String fallbackActivatedAt,
    String lastProviderError,
    String lastProviderErrorAt) {

  public HermesFallbackProfileStatus(
      String profileId,
      String expectedRoute,
      boolean healthy,
      boolean openAiAvailable,
      String cooldownUntil,
      String detail) {
    this(profileId, expectedRoute, healthy, openAiAvailable, cooldownUntil, detail, null, null, null, null, null);
  }
}
