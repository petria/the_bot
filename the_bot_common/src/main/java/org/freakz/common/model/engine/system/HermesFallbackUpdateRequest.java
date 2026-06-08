package org.freakz.common.model.engine.system;

public record HermesFallbackUpdateRequest(String baseUrl, String model, Boolean enabled) {

  public HermesFallbackUpdateRequest {
    enabled = enabled != null && enabled;
  }
}
