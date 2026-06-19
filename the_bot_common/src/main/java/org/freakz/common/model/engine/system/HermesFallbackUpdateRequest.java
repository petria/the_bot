package org.freakz.common.model.engine.system;

public record HermesFallbackUpdateRequest(
    String baseUrl,
    String model,
    Boolean enabled,
    Integer contextWindow) {

  public HermesFallbackUpdateRequest(String baseUrl, String model, Boolean enabled) {
    this(baseUrl, model, enabled, null);
  }
}
