package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesFallbackModelsResponse(List<String> models, List<HermesFallbackModel> items) {

  public HermesFallbackModelsResponse(List<String> models) {
    this(models, models == null ? List.of() : models.stream()
        .map(model -> new HermesFallbackModel(model, "unknown", "tool support unknown", null, null))
        .toList());
  }
}
