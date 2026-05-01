package org.freakz.common.chat;

public final class ChatIdentityUtil {

  private ChatIdentityUtil() {
  }

  public static String sanitize(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    String sanitized = value.trim().toLowerCase().replaceAll("[^a-z0-9#:_-]+", "-");
    return sanitized.isBlank() ? fallback : sanitized;
  }

  public static String resolveProtocol(String networkRaw) {
    if (networkRaw == null) {
      return "chat";
    }

    String n = networkRaw.trim().toLowerCase();
    if (n.contains("discord")) {
      return "discord";
    }
    if (n.contains("telegram")) {
      return "telegram";
    }
    if (n.contains("irc")) {
      return "irc";
    }

    return sanitize(n, "chat");
  }

  public static String buildChatId(String protocol, String network, String chatType, String target) {
    return sanitize(protocol, "chat") + "/"
        + sanitize(network, "unknown") + "/"
        + sanitize(chatType, "channel") + "/"
        + sanitize(target, "unknown");
  }

  public static String extractTargetFromChatId(String chatId, String fallback) {
    if (chatId == null || chatId.isBlank() || !chatId.contains("/")) {
      return fallback;
    }

    String[] parts = chatId.split("/");
    if (parts.length == 0) {
      return fallback;
    }

    return sanitize(parts[parts.length - 1], fallback);
  }
}
