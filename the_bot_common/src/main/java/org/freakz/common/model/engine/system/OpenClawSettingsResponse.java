package org.freakz.common.model.engine.system;

import java.util.List;

public record OpenClawSettingsResponse(
    String currentInstanceId,
    String currentWsUrl,
    String currentOriginUrl,
    String currentHealthUrl,
    List<OpenClawInstanceOption> options) {
}
