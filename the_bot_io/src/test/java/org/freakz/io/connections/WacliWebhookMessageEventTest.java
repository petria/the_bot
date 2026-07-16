package org.freakz.io.connections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WacliWebhookMessageEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void parsesWacliParsedMessageJson() throws Exception {
    WacliWebhookMessageEvent event = WacliWebhookMessageEvent.from(objectMapper.readTree("""
        {
          "Chat": "358449125874@s.whatsapp.net",
          "ID": "message-id",
          "SenderJID": "358449125874@s.whatsapp.net",
          "Timestamp": "2026-05-10T10:00:00Z",
          "FromMe": false,
          "Text": "hello",
          "PushName": "Petri"
        }
        """));

    assertThat(event.getChatJid()).isEqualTo("358449125874@s.whatsapp.net");
    assertThat(event.getMessageId()).isEqualTo("message-id");
    assertThat(event.effectiveSenderJid()).isEqualTo("358449125874@s.whatsapp.net");
    assertThat(event.senderDisplayName()).isEqualTo("Petri");
    assertThat(event.getText()).isEqualTo("hello");
    assertThat(event.isPrivateChat()).isTrue();
  }

  @Test
  void composesJidWhenWacliSerializesChatAsObject() throws Exception {
    WacliWebhookMessageEvent event = WacliWebhookMessageEvent.from(objectMapper.readTree("""
        {
          "Chat": {
            "User": "1203630",
            "Server": "g.us"
          },
          "ID": "message-id",
          "Text": "hello"
        }
        """));

    assertThat(event.getChatJid()).isEqualTo("1203630@g.us");
    assertThat(event.isGroupChat()).isTrue();
  }

  @Test
  void parsesNestedImageMessagePayload() throws Exception {
    WacliWebhookMessageEvent event = WacliWebhookMessageEvent.from(objectMapper.readTree("""
        {
          "Info": {
            "Chat": {
              "User": "120363408176012025",
              "Server": "g.us"
            },
            "ID": "3EB01CA7A4E708CD3F3305",
            "Sender": {
              "User": "162251029934316",
              "Server": "lid"
            },
            "PushName": "Petri Airio",
            "Timestamp": "2026-07-04T20:46:22Z",
            "IsFromMe": false
          },
          "Message": {
            "imageMessage": {
              "URL": "https://mmg.whatsapp.net/o1/v/t62.7118-24/image.jpg",
              "Mimetype": "image/jpeg",
              "Caption": "test"
            }
          }
        }
        """));

    assertThat(event.getChatJid()).isEqualTo("120363408176012025@g.us");
    assertThat(event.getMessageId()).isEqualTo("3EB01CA7A4E708CD3F3305");
    assertThat(event.effectiveSenderJid()).isEqualTo("162251029934316@lid");
    assertThat(event.senderDisplayName()).isEqualTo("Petri Airio");
    assertThat(event.getText()).isEqualTo("test");
    assertThat(event.hasMedia()).isTrue();
    assertThat(event.getMediaUrl()).isEqualTo("https://mmg.whatsapp.net/o1/v/t62.7118-24/image.jpg");
    assertThat(event.getMediaContentType()).isEqualTo("image/jpeg");
  }

  @Test
  void parsesWhatsAppMentionedJidsFromExtendedTextContext() throws Exception {
    WacliWebhookMessageEvent event = WacliWebhookMessageEvent.from(objectMapper.readTree("""
        {
          "Chat": {"User": "1203630", "Server": "g.us"},
          "ID": "message-id",
          "Text": "@66134711775265 hello",
          "Message": {
            "extendedTextMessage": {
              "text": "@66134711775265 hello",
              "contextInfo": {
                "mentionedJid": ["66134711775265:2@lid"]
              }
            }
          }
        }
        """));

    assertThat(event.getMentionedJids()).containsExactly("66134711775265:2@lid");
  }

  @Test
  void parsesTopLevelMediaMetadataWithoutUrlAsMedia() throws Exception {
    WacliWebhookMessageEvent event = WacliWebhookMessageEvent.from(objectMapper.readTree("""
        {
          "Chat": "120363408176012025@g.us",
          "ID": "3EB0CF70BED7F3647D60D7",
          "SenderJID": "162251029934316:96@lid",
          "Timestamp": "2026-07-04T21:01:12Z",
          "FromMe": false,
          "Text": "test2",
          "Media": {
            "Type": "image",
            "Caption": "test2",
            "Filename": "",
            "MimeType": "image/jpeg",
            "DirectPath": "/o1/v/t24/f2/m234/image"
          },
          "PushName": "Petri Airio"
        }
        """));

    assertThat(event.hasMedia()).isTrue();
    assertThat(event.hasDownloadableMediaUrl()).isFalse();
    assertThat(event.getMediaContentType()).isEqualTo("image/jpeg");
    assertThat(event.getMediaDirectPath()).isEqualTo("/o1/v/t24/f2/m234/image");
    assertThat(event.getText()).isEqualTo("test2");
  }

  @Test
  void parsesQuotedReplyMetadata() throws Exception {
    WacliWebhookMessageEvent event = WacliWebhookMessageEvent.from(objectMapper.readTree("""
        {
          "Chat": "120363408176012025@g.us",
          "ID": "reply-message-id",
          "SenderJID": "162251029934316:96@lid",
          "ReplyToID": "original-message-id",
          "ReplyToSenderJID": "358449125874@s.whatsapp.net",
          "Text": "!hokan hello",
          "FromMe": false
        }
        """));

    assertThat(event.getReplyToMessageId()).isEqualTo("original-message-id");
    assertThat(event.getReplyToSenderJid()).isEqualTo("358449125874@s.whatsapp.net");
  }
}
