package org.freakz.common.users;

import java.util.List;

public final class BotPermission {

  public static final String ALL = "*";
  public static final String WEB_ADMIN = "web.admin";
  public static final String USERS_MANAGE = "users.manage";
  public static final String CONFIG_EDIT = "config.edit";
  public static final String COMMANDS_ADMIN = "commands.admin";
  public static final String OPENCLAW_USE = "openclaw.use";
  public static final String OPENCLAW_SEND_MESSAGE = "openclaw.tools.send-message";
  public static final String LOGS_READ_CURRENT_CHAT = "logs.read.current-chat";
  public static final String LOGS_READ_CURRENT_CHANNEL = "logs.read.current-channel";
  public static final String LOGS_READ_CURRENT_USER_DM = "logs.read.current-user-dm";
  public static final String LOGS_READ_ALL_PUBLIC_CHANNELS = "logs.read.all-public-channels";
  public static final String LOGS_READ_ALL_PRIVATE_CHATS = "logs.read.all-private-chats";
  public static final String LOGS_READ_ALL = "logs.read.all";

  private static final List<String> KNOWN = List.of(
      ALL,
      WEB_ADMIN,
      USERS_MANAGE,
      CONFIG_EDIT,
      COMMANDS_ADMIN,
      OPENCLAW_USE,
      OPENCLAW_SEND_MESSAGE,
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
