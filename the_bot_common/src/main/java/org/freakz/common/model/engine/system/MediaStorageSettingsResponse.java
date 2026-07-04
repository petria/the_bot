package org.freakz.common.model.engine.system;

public record MediaStorageSettingsResponse(
    Boolean enabled,
    String storageDir,
    String publicUrlPrefix,
    Integer maxFileSizeMb,
    Integer retentionDays,
    Boolean directoryExists,
    Boolean writable,
    String detail) {
}
