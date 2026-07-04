package org.freakz.common.model.engine.system;

public record MediaStorageUpdateRequest(
    Boolean enabled,
    String storageDir,
    Integer maxFileSizeMb,
    Integer retentionDays) {
}
