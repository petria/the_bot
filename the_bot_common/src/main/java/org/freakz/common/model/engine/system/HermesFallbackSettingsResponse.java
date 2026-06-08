package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesFallbackSettingsResponse(
    Boolean enabled,
    String baseUrl,
    String model,
    List<HermesFallbackProfileStatus> profiles) {
}
