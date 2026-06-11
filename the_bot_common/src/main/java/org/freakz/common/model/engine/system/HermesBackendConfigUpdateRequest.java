package org.freakz.common.model.engine.system;

import java.util.List;

public record HermesBackendConfigUpdateRequest(
    List<HermesBackendProfile> profiles,
    List<HermesAiRoute> routes) {
}
