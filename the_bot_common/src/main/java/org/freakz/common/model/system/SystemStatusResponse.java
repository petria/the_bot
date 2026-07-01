package org.freakz.common.model.system;

import java.time.Instant;
import java.util.List;

public record SystemStatusResponse(
    Instant checkedAt,
    List<SystemComponentStatus> components) {
}
