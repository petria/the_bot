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
}
