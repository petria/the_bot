package org.freakz.common.model.engine.system;

public record OpenClawInstanceOption(
    String id,
    String label,
    String host,
    String wsUrl,
    String originUrl,
    String healthUrl,
    boolean selected) {
}
