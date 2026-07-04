package org.freakz.engine.services.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.freakz.common.model.engine.system.MediaStorageUpdateRequest;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.junit.jupiter.api.Test;

class MediaStorageSettingsServiceTest {

  @Test
  void returnsDefaultRuntimeMediaSettings() {
    ConfigService configService = mock(ConfigService.class);
    when(configService.getConfigBooleanValue("media.storage.enabled", "THE_BOT_MEDIA_STORAGE_ENABLED", true))
        .thenReturn(true);
    when(configService.getConfigValue("media.storage.dir", "THE_BOT_MEDIA_STORAGE_DIR", "/runtime/media"))
        .thenReturn("/runtime/media");
    when(configService.getConfigIntValue("media.storage.max-file-size-mb", "THE_BOT_MEDIA_STORAGE_MAX_FILE_SIZE_MB", 25))
        .thenReturn(25);
    when(configService.getConfigIntValue("media.storage.retention-days", "THE_BOT_MEDIA_STORAGE_RETENTION_DAYS", 30))
        .thenReturn(30);
    when(configService.getConfigValue("the.bot.webPublicBaseUrl", "THE_BOT_WEB_PUBLIC_BASE_URL", "http://localhost:8091"))
        .thenReturn("https://bot.example");

    MediaStorageSettingsService service = new MediaStorageSettingsService(configService, mock(EnvValuesService.class));

    var settings = service.getSettings();

    assertThat(settings.enabled()).isTrue();
    assertThat(settings.storageDir()).isEqualTo("/runtime/media");
    assertThat(settings.publicUrlPrefix()).isEqualTo("https://bot.example/media");
    assertThat(settings.maxFileSizeMb()).isEqualTo(25);
    assertThat(settings.retentionDays()).isEqualTo(30);
  }

  @Test
  void rejectsRelativeStorageDir() {
    MediaStorageSettingsService service =
        new MediaStorageSettingsService(mock(ConfigService.class), mock(EnvValuesService.class));

    assertThatThrownBy(() -> service.update(new MediaStorageUpdateRequest(true, "runtime/media", 25, 30)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("absolute");
  }

  @Test
  void rejectsStorageDirOutsideRuntimeMount() {
    MediaStorageSettingsService service =
        new MediaStorageSettingsService(mock(ConfigService.class), mock(EnvValuesService.class));

    assertThatThrownBy(() -> service.update(new MediaStorageUpdateRequest(true, "/tmp/media", 25, 30)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("/runtime");
  }
}
