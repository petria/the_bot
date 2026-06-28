package org.freakz.common.users;

import java.util.Locale;

public final class ChannelPermissionUtil {

  private ChannelPermissionUtil() {
  }

  public static String viewPermission(String connectionType, String echoToAlias) {
    return BotPermission.CHANNELS_VIEW_PREFIX + connectionKey(connectionType) + "." + channelKey(echoToAlias);
  }

  public static String sendPermission(String connectionType, String echoToAlias) {
    return BotPermission.CHANNELS_SEND_PREFIX + connectionKey(connectionType) + "." + channelKey(echoToAlias);
  }

  public static String viewTypePermission(String connectionType) {
    return BotPermission.CHANNELS_VIEW_PREFIX + connectionKey(connectionType);
  }

  public static String sendTypePermission(String connectionType) {
    return BotPermission.CHANNELS_SEND_PREFIX + connectionKey(connectionType);
  }

  public static String connectionKey(String connectionType) {
    String value = connectionType == null ? "" : connectionType.trim().toLowerCase(Locale.ROOT);
    value = value.replaceAll("_connection$", "");
    return value.replaceAll("[^a-z0-9_-]", "_");
  }

  public static String channelKey(String echoToAlias) {
    String value = echoToAlias == null ? "" : echoToAlias.trim().toLowerCase(Locale.ROOT);
    return value.replaceAll("[^a-z0-9._-]", "_");
  }

  public static boolean isSupportedConnectionKey(String value) {
    String normalized = connectionKey(value);
    return "irc".equals(normalized)
        || "discord".equals(normalized)
        || "telegram".equals(normalized)
        || "whatsapp".equals(normalized);
  }
}
