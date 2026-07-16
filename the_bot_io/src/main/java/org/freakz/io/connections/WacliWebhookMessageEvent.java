package org.freakz.io.connections;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WacliWebhookMessageEvent {

  private final String chatJid;
  private final String messageId;
  private final String replyToMessageId;
  private final String replyToSenderJid;
  private final String senderJid;
  private final String pushName;
  private final String text;
  private final String mediaUrl;
  private final String mediaContentType;
  private final String mediaFileName;
  private final String mediaDirectPath;
  private final boolean fromMe;
  private final Instant timestamp;
  private final List<String> mentionedJids;

  public WacliWebhookMessageEvent(
      String chatJid,
      String messageId,
      String replyToMessageId,
      String replyToSenderJid,
      String senderJid,
      String pushName,
      String text,
      String mediaUrl,
      String mediaContentType,
      String mediaFileName,
      String mediaDirectPath,
      boolean fromMe,
      Instant timestamp,
      List<String> mentionedJids) {
    this.chatJid = chatJid;
    this.messageId = messageId;
    this.replyToMessageId = replyToMessageId;
    this.replyToSenderJid = replyToSenderJid;
    this.senderJid = senderJid;
    this.pushName = pushName;
    this.text = text;
    this.mediaUrl = mediaUrl;
    this.mediaContentType = mediaContentType;
    this.mediaFileName = mediaFileName;
    this.mediaDirectPath = mediaDirectPath;
    this.fromMe = fromMe;
    this.timestamp = timestamp;
    this.mentionedJids = mentionedJids == null ? List.of() : List.copyOf(mentionedJids);
  }

  public static WacliWebhookMessageEvent from(JsonNode root) {
    JsonNode info = first(root, "Info", "info");
    JsonNode message = first(root, "Message", "message");
    JsonNode media = firstMedia(root, message);

    String chatJid = jidValue(first(root, "Chat", "chat", "chat_jid", "chatJid"));
    if (chatJid == null) {
      chatJid = jidValue(first(info, "Chat", "chat", "chat_jid", "chatJid"));
    }
    String messageId = textValue(first(root, "ID", "id", "message_id", "messageId"));
    if (messageId == null) {
      messageId = textValue(first(info, "ID", "id", "message_id", "messageId"));
    }
    String senderJid = jidValue(first(root, "SenderJID", "sender_jid", "senderJid", "Sender", "sender"));
    if (senderJid == null) {
      senderJid = jidValue(first(info, "SenderJID", "sender_jid", "senderJid", "Sender", "sender"));
    }
    String replyToMessageId = textValue(first(root, "ReplyToID", "reply_to_id", "replyToId", "ReplyToMessageID", "replyToMessageId"));
    if (replyToMessageId == null) {
      replyToMessageId = textValue(first(info, "ReplyToID", "reply_to_id", "replyToId", "ReplyToMessageID", "replyToMessageId"));
    }
    String replyToSenderJid = jidValue(first(root, "ReplyToSenderJID", "reply_to_sender_jid", "replyToSenderJid"));
    if (replyToSenderJid == null) {
      replyToSenderJid = jidValue(first(info, "ReplyToSenderJID", "reply_to_sender_jid", "replyToSenderJid"));
    }
    String pushName = textValue(first(root, "PushName", "push_name", "pushName"));
    if (pushName == null) {
      pushName = textValue(first(info, "PushName", "push_name", "pushName"));
    }
    String text = firstNonBlank(
        textValue(first(root, "Text", "text", "DisplayText", "display_text", "displayText", "Conversation", "conversation")),
        textValue(first(message, "Conversation", "conversation")),
        textValue(first(first(message, "ExtendedTextMessage", "extendedTextMessage"), "Text", "text")),
        textValue(first(media, "Caption", "caption")));

    String mediaUrl = textValue(first(root, "MediaURL", "MediaUrl", "media_url", "mediaUrl", "URL", "url"));
    if (mediaUrl == null) {
      mediaUrl = textValue(first(media, "URL", "Url", "url", "MediaURL", "MediaUrl", "media_url", "mediaUrl"));
    }
    String mediaContentType = textValue(first(root, "MediaContentType", "mediaContentType", "mimeType", "mimetype", "Mimetype"));
    if (mediaContentType == null) {
      mediaContentType = textValue(first(media, "ContentType", "contentType", "MimeType", "mimeType", "mimetype", "Mimetype"));
    }
    String mediaFileName = textValue(first(root, "MediaFileName", "mediaFileName", "filename", "fileName", "FileName"));
    if (mediaFileName == null) {
      mediaFileName = textValue(first(media, "FileName", "fileName", "filename", "Name", "name", "Title", "title"));
    }
    String mediaDirectPath = textValue(first(media, "DirectPath", "directPath"));
    boolean fromMe = booleanValue(firstNonNull(first(root, "FromMe", "from_me", "fromMe", "IsFromMe", "isFromMe"), first(info, "FromMe", "from_me", "fromMe", "IsFromMe", "isFromMe")));
    Instant timestamp = instantValue(firstNonNull(first(root, "Timestamp", "timestamp"), first(info, "Timestamp", "timestamp")));
    List<String> mentionedJids = jidValues(first(
        first(first(message, "ExtendedTextMessage", "extendedTextMessage"), "ContextInfo", "contextInfo"),
        "MentionedJID", "mentionedJid", "MentionedJIDs", "mentionedJids"));
    return new WacliWebhookMessageEvent(chatJid, messageId, replyToMessageId, replyToSenderJid, senderJid, pushName, text, mediaUrl, mediaContentType, mediaFileName, mediaDirectPath, fromMe, timestamp, mentionedJids);
  }

  public static String fieldSummary(JsonNode root) {
    if (root == null || root.isNull()) {
      return "<empty>";
    }
    List<String> fields = new ArrayList<>();
    collectFieldSummary(fields, "", root, 0);
    return String.join(",", fields);
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

  public String getReplyToMessageId() {
    return replyToMessageId;
  }

  public String getReplyToSenderJid() {
    return replyToSenderJid;
  }

  public String getPushName() {
    return pushName;
  }

  public String getText() {
    return text;
  }

  public List<String> getMentionedJids() {
    return mentionedJids;
  }

  public boolean hasMedia() {
    return hasDownloadableMediaUrl() || mediaContentType != null || mediaDirectPath != null;
  }

  public boolean hasDownloadableMediaUrl() {
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

  public String getMediaDirectPath() {
    return mediaDirectPath;
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

  private static JsonNode firstNonNull(JsonNode... nodes) {
    for (JsonNode node : nodes) {
      if (node != null && !node.isNull()) {
        return node;
      }
    }
    return null;
  }

  private static JsonNode firstMedia(JsonNode root, JsonNode message) {
    JsonNode direct = first(root,
        "Media", "media", "Attachment", "attachment",
        "ImageMessage", "imageMessage", "VideoMessage", "videoMessage",
        "DocumentMessage", "documentMessage", "StickerMessage", "stickerMessage");
    if (direct != null) {
      return direct;
    }
    return first(message,
        "ImageMessage", "imageMessage", "VideoMessage", "videoMessage",
        "DocumentMessage", "documentMessage", "StickerMessage", "stickerMessage");
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

  private static List<String> jidValues(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of();
    }
    List<String> values = new ArrayList<>();
    if (node.isArray()) {
      node.forEach(item -> {
        String value = jidValue(item);
        if (value != null) {
          values.add(value);
        }
      });
    } else {
      String value = jidValue(node);
      if (value != null) {
        values.add(value);
      }
    }
    return values;
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

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private static void collectFieldSummary(List<String> fields, String prefix, JsonNode node, int depth) {
    if (node == null || !node.isObject() || depth > 2) {
      return;
    }
    Iterator<String> names = node.fieldNames();
    while (names.hasNext()) {
      String name = names.next();
      String path = prefix.isBlank() ? name : prefix + "." + name;
      fields.add(path);
      JsonNode child = node.get(name);
      if (child != null && child.isObject()) {
        collectFieldSummary(fields, path, child, depth + 1);
      }
    }
  }
}
