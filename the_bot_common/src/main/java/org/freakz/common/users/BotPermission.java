package org.freakz.common.users;

import java.util.List;

public final class BotPermission {

  public static final String ALL = "*";
  public static final String WEB_USER = "web.user";
  public static final String WEB_ADMIN = "web.admin";
  public static final String USERS_MANAGE = "users.manage";
  public static final String CONFIG_EDIT = "config.edit";
  public static final String COMMANDS_ADMIN = "commands.admin";
  public static final String HOWTO_USE = "howto.use";
  public static final String HERMES_USE = "hermes.use";
  public static final String CHANNELS_VIEW_ALL = "channels.view.all";
  public static final String CHANNELS_SEND_ALL = "channels.send.all";
  public static final String CHANNELS_VIEW_PREFIX = "channels.view.";
  public static final String CHANNELS_SEND_PREFIX = "channels.send.";
  public static final String CHANNELS_VIEW_IRC = "channels.view.irc";
  public static final String CHANNELS_SEND_IRC = "channels.send.irc";
  public static final String CHANNELS_VIEW_DISCORD = "channels.view.discord";
  public static final String CHANNELS_SEND_DISCORD = "channels.send.discord";
  public static final String CHANNELS_VIEW_TELEGRAM = "channels.view.telegram";
  public static final String CHANNELS_SEND_TELEGRAM = "channels.send.telegram";
  public static final String CHANNELS_VIEW_WHATSAPP = "channels.view.whatsapp";
  public static final String CHANNELS_SEND_WHATSAPP = "channels.send.whatsapp";
  public static final String LOGS_READ_CURRENT_CHAT = "logs.read.current-chat";
  public static final String LOGS_READ_CURRENT_CHANNEL = "logs.read.current-channel";
  public static final String LOGS_READ_CURRENT_USER_DM = "logs.read.current-user-dm";
  public static final String LOGS_READ_ALL_PUBLIC_CHANNELS = "logs.read.all-public-channels";
  public static final String LOGS_READ_ALL_PRIVATE_CHATS = "logs.read.all-private-chats";
  public static final String LOGS_READ_ALL = "logs.read.all";

  private static final List<String> KNOWN = List.of(
      ALL,
      WEB_USER,
      WEB_ADMIN,
      USERS_MANAGE,
      CONFIG_EDIT,
      COMMANDS_ADMIN,
      HOWTO_USE,
      HERMES_USE,
      CHANNELS_VIEW_ALL,
      CHANNELS_SEND_ALL,
      CHANNELS_VIEW_IRC,
      CHANNELS_SEND_IRC,
      CHANNELS_VIEW_DISCORD,
      CHANNELS_SEND_DISCORD,
      CHANNELS_VIEW_TELEGRAM,
      CHANNELS_SEND_TELEGRAM,
      CHANNELS_VIEW_WHATSAPP,
      CHANNELS_SEND_WHATSAPP,
      LOGS_READ_CURRENT_CHAT,
      LOGS_READ_CURRENT_CHANNEL,
      LOGS_READ_CURRENT_USER_DM,
      LOGS_READ_ALL_PUBLIC_CHANNELS,
      LOGS_READ_ALL_PRIVATE_CHATS,
      LOGS_READ_ALL
  );

  private BotPermission() {
  }

  public static List<String> known() {
    return KNOWN;
  }
}
