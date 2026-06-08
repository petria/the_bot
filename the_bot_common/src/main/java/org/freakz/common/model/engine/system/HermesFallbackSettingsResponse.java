package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesFallbackSettingsResponse(
    String baseUrl,
    String model,
    List<HermesFallbackProfileStatus> profiles) {
}
