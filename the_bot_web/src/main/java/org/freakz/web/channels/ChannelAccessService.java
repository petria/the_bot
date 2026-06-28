package org.freakz.web.channels;

import org.freakz.common.users.BotPermission;
import org.freakz.common.users.ChannelPermissionUtil;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChannelAccessService {

  private static final String LEGACY_LIVE_CHANNELS_VIEW_ALL = "live-channels.view.all";
  private static final String LEGACY_LIVE_CHANNELS_SEND_ALL = "live-channels.send.all";
  private static final String LEGACY_LIVE_CHANNELS_VIEW_PREFIX = "live-channels.view.";
  private static final String LEGACY_LIVE_CHANNELS_SEND_PREFIX = "live-channels.send.";

  public boolean canView(BotUserPrincipal principal, String connectionType, String echoToAlias) {
    String connectionKey = connectionKey(connectionType);
    String channelKey = channelKey(echoToAlias);
    return has(principal, BotPermission.CHANNELS_VIEW_ALL)
        || has(principal, BotPermission.CHANNELS_VIEW_PREFIX + connectionKey)
        || has(principal, viewPermission(connectionType, echoToAlias))
        || hasLegacyView(principal, channelKey);
  }

  public boolean canSend(BotUserPrincipal principal, String connectionType, String echoToAlias) {
    String connectionKey = connectionKey(connectionType);
    String channelKey = channelKey(echoToAlias);
    return canView(principal, connectionType, echoToAlias)
        && (has(principal, BotPermission.CHANNELS_SEND_ALL)
            || has(principal, BotPermission.CHANNELS_SEND_PREFIX + connectionKey)
            || has(principal, sendPermission(connectionType, echoToAlias))
            || hasLegacySend(principal, channelKey));
  }

  public boolean hasAnyChannelView(BotUserPrincipal principal) {
    if (principal == null) {
      return false;
    }
    return permissions(principal).stream().anyMatch(permission ->
        BotPermission.ALL.equals(permission)
            || BotPermission.CHANNELS_VIEW_ALL.equals(permission)
            || permission.startsWith(BotPermission.CHANNELS_VIEW_PREFIX)
            || LEGACY_LIVE_CHANNELS_VIEW_ALL.equals(permission)
            || permission.startsWith(LEGACY_LIVE_CHANNELS_VIEW_PREFIX));
  }

  public void requireView(BotUserPrincipal principal, String connectionType, String echoToAlias) {
    if (!canView(principal, connectionType, echoToAlias)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this channel");
    }
  }

  public void requireSend(BotUserPrincipal principal, String connectionType, String echoToAlias) {
    if (!canSend(principal, connectionType, echoToAlias)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to send to this channel");
    }
  }

  public String viewPermission(String connectionType, String echoToAlias) {
    return ChannelPermissionUtil.viewPermission(connectionType, echoToAlias);
  }

  public String sendPermission(String connectionType, String echoToAlias) {
    return ChannelPermissionUtil.sendPermission(connectionType, echoToAlias);
  }

  public String connectionKey(String connectionType) {
    return ChannelPermissionUtil.connectionKey(connectionType);
  }

  public String channelKey(String echoToAlias) {
    return ChannelPermissionUtil.channelKey(echoToAlias);
  }

  private boolean hasLegacyView(BotUserPrincipal principal, String channelKey) {
    return has(principal, LEGACY_LIVE_CHANNELS_VIEW_ALL)
        || has(principal, LEGACY_LIVE_CHANNELS_VIEW_PREFIX + channelKey);
  }

  private boolean hasLegacySend(BotUserPrincipal principal, String channelKey) {
    return has(principal, LEGACY_LIVE_CHANNELS_SEND_ALL)
        || has(principal, LEGACY_LIVE_CHANNELS_SEND_PREFIX + channelKey);
  }

  private boolean has(BotUserPrincipal principal, String permission) {
    Set<String> permissions = permissions(principal);
    String requested = normalize(permission);
    return permissions.contains(BotPermission.ALL) || permissions.contains(requested);
  }

  private Set<String> permissions(BotUserPrincipal principal) {
    if (principal == null || principal.getPermissions() == null) {
      return Set.of();
    }
    return principal.getPermissions().stream()
        .map(this::normalize)
        .filter(permission -> permission != null && !permission.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }

  private String normalize(String permission) {
    return permission == null ? null : permission.trim().toLowerCase(Locale.ROOT);
  }
}
