package org.freakz.web.livechannels;

import org.freakz.common.users.BotPermission;
import org.freakz.web.security.BotUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LiveChannelAccessService {

  public boolean canView(BotUserPrincipal principal, String echoToAlias) {
    return hasAny(principal, BotPermission.WEB_ADMIN, BotPermission.LIVE_CHANNELS_VIEW_ALL)
        || has(principal, BotPermission.LIVE_CHANNELS_VIEW_PREFIX + channelKey(echoToAlias));
  }

  public boolean canSend(BotUserPrincipal principal, String echoToAlias) {
    return canView(principal, echoToAlias)
        && (hasAny(principal, BotPermission.WEB_ADMIN, BotPermission.LIVE_CHANNELS_SEND_ALL)
            || has(principal, BotPermission.LIVE_CHANNELS_SEND_PREFIX + channelKey(echoToAlias)));
  }

  public boolean hasAnyLiveChannelView(BotUserPrincipal principal) {
    if (principal == null) {
      return false;
    }
    if (hasAny(principal, BotPermission.WEB_ADMIN, BotPermission.LIVE_CHANNELS_VIEW_ALL)) {
      return true;
    }
    return permissions(principal).stream().anyMatch(permission -> permission.startsWith(BotPermission.LIVE_CHANNELS_VIEW_PREFIX));
  }

  public void requireView(BotUserPrincipal principal, String echoToAlias) {
    if (!canView(principal, echoToAlias)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this live channel");
    }
  }

  public void requireSend(BotUserPrincipal principal, String echoToAlias) {
    if (!canSend(principal, echoToAlias)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to send to this live channel");
    }
  }

  public String viewPermission(String echoToAlias) {
    return BotPermission.LIVE_CHANNELS_VIEW_PREFIX + channelKey(echoToAlias);
  }

  public String sendPermission(String echoToAlias) {
    return BotPermission.LIVE_CHANNELS_SEND_PREFIX + channelKey(echoToAlias);
  }

  public String channelKey(String echoToAlias) {
    String value = echoToAlias == null ? "" : echoToAlias.trim().toLowerCase(Locale.ROOT);
    return value.replaceAll("[^a-z0-9._-]", "_");
  }

  private boolean hasAny(BotUserPrincipal principal, String... permissions) {
    for (String permission : permissions) {
      if (has(principal, permission)) {
        return true;
      }
    }
    return false;
  }

  private boolean has(BotUserPrincipal principal, String permission) {
    Set<String> permissions = permissions(principal);
    String requested = normalize(permission);
    return permissions.contains(BotPermission.ALL)
        || permissions.contains(requested)
        || (BotPermission.WEB_USER.equals(requested) && permissions.contains(BotPermission.WEB_ADMIN));
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
