package org.freakz.common.model.engine.system;

public record HermesGlobalOverrideSettings(
    Boolean enabled,
    String provider,
    String baseUrl,
    String model,
    Integer contextWindow,
    Boolean healthy,
    String detail,
    String activatedAt,
    String updatedBy,
    String validationStatus,
    String lastValidatedAt) {
}
