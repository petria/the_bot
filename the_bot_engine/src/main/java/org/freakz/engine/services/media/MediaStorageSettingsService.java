package org.freakz.engine.services.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.model.engine.system.MediaStorageUpdateRequest;
import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.springframework.stereotype.Service;

@Service
public class MediaStorageSettingsService {

  private static final String ENABLED_KEY = "media.storage.enabled";
  private static final String STORAGE_DIR_KEY = "media.storage.dir";
  private static final String MAX_FILE_SIZE_MB_KEY = "media.storage.max-file-size-mb";
  private static final String RETENTION_DAYS_KEY = "media.storage.retention-days";

  private static final String ENABLED_ENV = "THE_BOT_MEDIA_STORAGE_ENABLED";
  private static final String STORAGE_DIR_ENV = "THE_BOT_MEDIA_STORAGE_DIR";
  private static final String MAX_FILE_SIZE_MB_ENV = "THE_BOT_MEDIA_STORAGE_MAX_FILE_SIZE_MB";
  private static final String RETENTION_DAYS_ENV = "THE_BOT_MEDIA_STORAGE_RETENTION_DAYS";

  private static final String DEFAULT_STORAGE_DIR = "/runtime/media";
  private static final int DEFAULT_MAX_FILE_SIZE_MB = 25;
  private static final int DEFAULT_RETENTION_DAYS = 30;

  private final ConfigService configService;
  private final EnvValuesService envValuesService;

  public MediaStorageSettingsService(ConfigService configService, EnvValuesService envValuesService) {
    this.configService = configService;
    this.envValuesService = envValuesService;
  }

  public MediaStorageSettingsResponse getSettings() {
    return responseFor(
        configService.getConfigBooleanValue(ENABLED_KEY, ENABLED_ENV, true),
        storageDir(),
        configService.getConfigIntValue(MAX_FILE_SIZE_MB_KEY, MAX_FILE_SIZE_MB_ENV, DEFAULT_MAX_FILE_SIZE_MB),
        configService.getConfigIntValue(RETENTION_DAYS_KEY, RETENTION_DAYS_ENV, DEFAULT_RETENTION_DAYS));
  }

  public MediaStorageSettingsResponse update(MediaStorageUpdateRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Media storage settings are required");
    }

    boolean enabled = request.enabled() == null || request.enabled();
    String storageDir = normalizeStorageDir(request.storageDir());
    int maxFileSizeMb = positiveInt(request.maxFileSizeMb(), "Max file size MB");
    int retentionDays = positiveInt(request.retentionDays(), "Retention days");

    ensureSupportedStorageDir(storageDir);
    ensureDirectory(storageDir);

    User user = User.builder().username("bot-web").name("bot-web").build();
    envValuesService.setEnvValue(ENABLED_KEY, Boolean.toString(enabled), user);
    envValuesService.setEnvValue(STORAGE_DIR_KEY, storageDir, user);
    envValuesService.setEnvValue(MAX_FILE_SIZE_MB_KEY, Integer.toString(maxFileSizeMb), user);
    envValuesService.setEnvValue(RETENTION_DAYS_KEY, Integer.toString(retentionDays), user);

    return responseFor(enabled, storageDir, maxFileSizeMb, retentionDays);
  }

  private MediaStorageSettingsResponse responseFor(
      boolean enabled,
      String storageDir,
      int maxFileSizeMb,
      int retentionDays) {
    String detail = null;
    boolean exists = false;
    boolean writable = false;
    try {
      ensureSupportedStorageDir(storageDir);
      Path path = Path.of(storageDir);
      exists = Files.exists(path);
      writable = exists && Files.isDirectory(path) && Files.isWritable(path);
      if (!exists) {
        detail = "Directory does not exist yet";
      } else if (!Files.isDirectory(path)) {
        detail = "Path exists but is not a directory";
      } else if (!writable) {
        detail = "Directory is not writable by bot-engine";
      }
    } catch (RuntimeException e) {
      detail = e.getMessage();
    }

    return new MediaStorageSettingsResponse(
        enabled,
        storageDir,
        publicUrlPrefix(),
        maxFileSizeMb,
        retentionDays,
        exists,
        writable,
        detail);
  }

  private String storageDir() {
    return normalizeStorageDir(configService.getConfigValue(STORAGE_DIR_KEY, STORAGE_DIR_ENV, DEFAULT_STORAGE_DIR));
  }

  private String publicUrlPrefix() {
    String baseUrl = configService.getConfigValue(
        "the.bot.webPublicBaseUrl",
        "THE_BOT_WEB_PUBLIC_BASE_URL",
        "http://localhost:8091");
    return trimTrailingSlash(baseUrl) + "/media";
  }

  private String normalizeStorageDir(String value) {
    String normalized = value == null || value.isBlank() ? DEFAULT_STORAGE_DIR : value.trim();
    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private void ensureSupportedStorageDir(String storageDir) {
    Path path = Path.of(storageDir);
    if (!path.isAbsolute()) {
      throw new IllegalArgumentException("Storage directory must be absolute");
    }
    Path normalized = path.normalize();
    if (!normalized.startsWith("/runtime")) {
      throw new IllegalArgumentException("Storage directory must be under /runtime");
    }
  }

  private void ensureDirectory(String storageDir) {
    try {
      Files.createDirectories(Path.of(storageDir));
    } catch (IOException e) {
      throw new IllegalStateException("Could not create media storage directory: " + e.getMessage(), e);
    }
  }

  private int positiveInt(Integer value, String fieldName) {
    int normalized = value == null ? 0 : value;
    if (normalized <= 0) {
      throw new IllegalArgumentException(fieldName + " must be greater than zero");
    }
    return normalized;
  }

  private String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.trim().replaceFirst("/+$", "");
  }
}
