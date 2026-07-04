package org.freakz.io.connections;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public class WacliWebhookMessageEvent {

  private final String chatJid;
  private final String messageId;
  private final String senderJid;
  private final String pushName;
  private final String text;
  private final String mediaUrl;
  private final String mediaContentType;
  private final String mediaFileName;
  private final boolean fromMe;
  private final Instant timestamp;

  public WacliWebhookMessageEvent(
      String chatJid,
      String messageId,
      String senderJid,
      String pushName,
      String text,
      String mediaUrl,
      String mediaContentType,
      String mediaFileName,
      boolean fromMe,
      Instant timestamp) {
    this.chatJid = chatJid;
    this.messageId = messageId;
    this.senderJid = senderJid;
    this.pushName = pushName;
    this.text = text;
    this.mediaUrl = mediaUrl;
    this.mediaContentType = mediaContentType;
    this.mediaFileName = mediaFileName;
    this.fromMe = fromMe;
    this.timestamp = timestamp;
  }

  public static WacliWebhookMessageEvent from(JsonNode root) {
    String chatJid = jidValue(first(root, "Chat", "chat", "chat_jid", "chatJid"));
    String messageId = textValue(first(root, "ID", "id", "message_id", "messageId"));
    String senderJid = textValue(first(root, "SenderJID", "sender_jid", "senderJid"));
    String pushName = textValue(first(root, "PushName", "push_name", "pushName"));
    String text = textValue(first(root, "Text", "text", "DisplayText", "display_text", "displayText"));
    JsonNode media = first(root, "Media", "media", "Attachment", "attachment");
    String mediaUrl = textValue(first(root, "MediaURL", "MediaUrl", "media_url", "mediaUrl", "URL", "url"));
    if (mediaUrl == null) {
      mediaUrl = textValue(first(media, "URL", "url", "MediaURL", "MediaUrl", "media_url", "mediaUrl"));
    }
    String mediaContentType = textValue(first(root, "MediaContentType", "mediaContentType", "mimeType", "mimetype", "Mimetype"));
    if (mediaContentType == null) {
      mediaContentType = textValue(first(media, "ContentType", "contentType", "MimeType", "mimeType", "mimetype"));
    }
    String mediaFileName = textValue(first(root, "MediaFileName", "mediaFileName", "filename", "fileName", "FileName"));
    if (mediaFileName == null) {
      mediaFileName = textValue(first(media, "FileName", "fileName", "filename", "Name", "name"));
    }
    boolean fromMe = booleanValue(first(root, "FromMe", "from_me", "fromMe"));
    Instant timestamp = instantValue(first(root, "Timestamp", "timestamp"));
    return new WacliWebhookMessageEvent(chatJid, messageId, senderJid, pushName, text, mediaUrl, mediaContentType, mediaFileName, fromMe, timestamp);
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

  public boolean hasMedia() {
    return mediaUrl != null && !mediaUrl.isBlank();
  }

  public String getMediaUrl() {
    return mediaUrl;
  }

  public String getMediaContentType() {
    return mediaContentType;
  }

  public String getMediaFileName() {
    return mediaFileName;
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
