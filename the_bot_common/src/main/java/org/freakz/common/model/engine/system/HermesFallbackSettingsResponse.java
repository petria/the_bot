package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesFallbackSettingsResponse(
    boolean enabled,
    String baseUrl,
    String model,
    List<HermesFallbackProfileStatus> profiles) {
}
