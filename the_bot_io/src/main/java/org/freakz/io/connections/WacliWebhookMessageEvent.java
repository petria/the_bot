package org.freakz.io.connections;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public class WacliWebhookMessageEvent {

  private final String chatJid;
  private final String messageId;
  private final String senderJid;
  private final String pushName;
  private final String text;
  private final boolean fromMe;
  private final Instant timestamp;

  public WacliWebhookMessageEvent(
      String chatJid,
      String messageId,
      String senderJid,
      String pushName,
      String text,
      boolean fromMe,
      Instant timestamp) {
    this.chatJid = chatJid;
    this.messageId = messageId;
    this.senderJid = senderJid;
    this.pushName = pushName;
    this.text = text;
    this.fromMe = fromMe;
    this.timestamp = timestamp;
  }

  public static WacliWebhookMessageEvent from(JsonNode root) {
    String chatJid = jidValue(first(root, "Chat", "chat", "chat_jid", "chatJid"));
    String messageId = textValue(first(root, "ID", "id", "message_id", "messageId"));
    String senderJid = textValue(first(root, "SenderJID", "sender_jid", "senderJid"));
    String pushName = textValue(first(root, "PushName", "push_name", "pushName"));
    String text = textValue(first(root, "Text", "text", "DisplayText", "display_text", "displayText"));
    boolean fromMe = booleanValue(first(root, "FromMe", "from_me", "fromMe"));
    Instant timestamp = instantValue(first(root, "Timestamp", "timestamp"));
    return new WacliWebhookMessageEvent(chatJid, messageId, senderJid, pushName, text, fromMe, timestamp);
  }

  public boolean isGroupChat() {
    return chatJid != null && chatJid.endsWith("@g.us");
  }

  public boolean isPrivateChat() {
    return !isGroupChat();
  }

  public String effectiveSenderJid() {
    if (senderJid != null && !senderJid.isBlank()) {
      return senderJid;
    }
    return chatJid;
  }

  public String senderDisplayName() {
    if (pushName != null && !pushName.isBlank()) {
      return pushName;
    }
    return effectiveSenderJid();
  }

  public String getChatJid() {
    return chatJid;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getSenderJid() {
    return senderJid;
  }

  public String getPushName() {
    return pushName;
  }

  public String getText() {
    return text;
  }

  public boolean isFromMe() {
    return fromMe;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  private static JsonNode first(JsonNode root, String... names) {
    if (root == null) {
      return null;
    }
    for (String name : names) {
      JsonNode value = root.get(name);
      if (value != null && !value.isNull()) {
        return value;
      }
    }
    return null;
  }

  private static String jidValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return blankToNull(node.asText());
    }
    String user = textValue(first(node, "User", "user"));
    String server = textValue(first(node, "Server", "server"));
    if (user != null && server != null) {
      return user + "@" + server;
    }
    return blankToNull(node.asText());
  }

  private static String textValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    return blankToNull(node.asText());
  }

  private static boolean booleanValue(JsonNode node) {
    return node != null && !node.isNull() && node.asBoolean(false);
  }

  private static Instant instantValue(JsonNode node) {
    String value = textValue(node);
    if (value == null) {
      return Instant.now();
    }
    try {
      return Instant.parse(value);
    } catch (Exception ignored) {
      return Instant.now();
    }
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
