package org.freakz.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.freakz.common.media.MediaStore;
import org.freakz.common.media.MediaStoreSource;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.model.users.User;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.common.urlarchive.UrlArchiveSource;
import org.freakz.common.urlarchive.UrlArchiveStore;
import org.freakz.common.users.BotPermission;
import org.freakz.web.channels.ChannelAccessService;
import org.freakz.web.security.BotUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import tools.jackson.databind.json.JsonMapper;

class LiveMediaControllerTest {

  @TempDir
  Path tempDir;

  @Test
  void returnsOnlyItemsVisibleToPrincipalChannelPermissions() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    new MediaStore(tempDir, mapper).create(
        new byte[] {1, 2, 3},
        "image/png",
        "visible.png",
        Duration.ofDays(1),
        new MediaStoreSource("irc", "IRCNet", "IRC-VISIBLE", "#visible", "petria"));
    new MediaStore(tempDir, mapper).create(
        new byte[] {4, 5, 6},
        "image/png",
        "hidden.png",
        Duration.ofDays(1),
        new MediaStoreSource("discord", "Discord", "DISCORD-HIDDEN", "hidden", "petria"));
    new UrlArchiveStore(tempDir, mapper).create(
        "https://example.com/visible",
        "Web",
        "Visible URL",
        null,
        null,
        null,
        null,
        null,
        Duration.ofDays(1),
        new UrlArchiveSource("irc", "IRCNet", "IRC-VISIBLE", "#visible", "petria"));
    new UrlArchiveStore(tempDir, mapper).create(
        "https://example.com/hidden",
        "Web",
        "Hidden URL",
        null,
        null,
        null,
        null,
        null,
        Duration.ofDays(1),
        new UrlArchiveSource("irc", "IRCNet", "IRC-HIDDEN", "#hidden", "petria"));

    LiveMediaController.LiveMediaResponse response = controller(mapper, settings(true)).getLiveMedia(
        principal(BotPermission.WEB_USER, "channels.view.irc.irc-visible"));

    assertThat(response.enabled()).isTrue();
    assertThat(response.items())
        .extracting(LiveMediaController.LiveMediaItem::type)
        .containsExactlyInAnyOrder("media", "url");
    assertThat(response.items())
        .extracting(LiveMediaController.LiveMediaItem::sourceChannelAlias)
        .containsOnly("IRC-VISIBLE");
    assertThat(response.items())
        .anySatisfy(item -> {
          assertThat(item.type()).isEqualTo("media");
          assertThat(item.originalFileName()).isEqualTo("visible.png");
        })
        .anySatisfy(item -> {
          assertThat(item.type()).isEqualTo("url");
          assertThat(item.url()).isEqualTo("https://example.com/visible");
        });
  }

  @Test
  void typeWidePermissionShowsAllItemsForThatConnectionType() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    new UrlArchiveStore(tempDir, mapper).create(
        "https://example.com/irc",
        "Web",
        "IRC URL",
        null,
        null,
        null,
        null,
        null,
        Duration.ofDays(1),
        new UrlArchiveSource("irc", "IRCNet", "IRC-ONE", "#one", "petria"));
    new UrlArchiveStore(tempDir, mapper).create(
        "https://example.com/telegram",
        "Web",
        "Telegram URL",
        null,
        null,
        null,
        null,
        null,
        Duration.ofDays(1),
        new UrlArchiveSource("telegram", "Telegram", "TELEGRAM-ONE", "one", "petria"));

    LiveMediaController.LiveMediaResponse response = controller(mapper, settings(true)).getLiveMedia(
        principal(BotPermission.WEB_USER, BotPermission.CHANNELS_VIEW_IRC));

    assertThat(response.items())
        .extracting(LiveMediaController.LiveMediaItem::sourceChannelAlias)
        .containsExactly("IRC-ONE");
  }

  @Test
  void recordsWithoutSourceAliasAreHidden() throws Exception {
    JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    new UrlArchiveStore(tempDir, mapper).create(
        "https://example.com/missing-source",
        "Web",
        "Missing Source",
        null,
        null,
        null,
        null,
        null,
        Duration.ofDays(1),
        new UrlArchiveSource("irc", "IRCNet", null, "#missing", "petria"));

    LiveMediaController.LiveMediaResponse response = controller(mapper, settings(true)).getLiveMedia(
        principal(BotPermission.ALL));

    assertThat(response.items()).isEmpty();
  }

  @Test
  void disabledMediaStorageReturnsEmptyList() {
    LiveMediaController.LiveMediaResponse response =
        controller(JsonMapper.builder().findAndAddModules().build(), settings(false)).getLiveMedia(
            principal(BotPermission.ALL));

    assertThat(response.enabled()).isFalse();
    assertThat(response.items()).isEmpty();
    assertThat(response.detail()).isEqualTo("Media storage is disabled");
  }

  @Test
  void upstreamFailureReturnsUnavailableResponse() {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.getMediaStorageSettings()).thenThrow(new IllegalStateException("engine unavailable"));

    LiveMediaController.LiveMediaResponse response = new LiveMediaController(
        engineClient,
        JsonMapper.builder().findAndAddModules().build(),
        new ChannelAccessService()).getLiveMedia(principal(BotPermission.ALL));

    assertThat(response.enabled()).isFalse();
    assertThat(response.items()).isEmpty();
    assertThat(response.detail()).contains("engine unavailable");
  }

  private LiveMediaController controller(JsonMapper mapper, MediaStorageSettingsResponse settings) {
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.getMediaStorageSettings()).thenReturn(ResponseEntity.ok(settings));
    return new LiveMediaController(engineClient, mapper, new ChannelAccessService());
  }

  private MediaStorageSettingsResponse settings(boolean enabled) {
    return new MediaStorageSettingsResponse(
        enabled,
        tempDir.toString(),
        "https://example.test/media",
        25,
        30,
        true,
        true,
        null);
  }

  private BotUserPrincipal principal(String... permissions) {
    User user = User.builder()
        .username("test")
        .password("hash")
        .permissions(List.of(permissions))
        .build();
    return BotUserPrincipal.from(user, List.of(permissions).stream()
        .map(SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList());
  }
}
