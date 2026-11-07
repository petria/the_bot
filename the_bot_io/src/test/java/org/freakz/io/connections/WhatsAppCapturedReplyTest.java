package org.freakz.io.connections;

import com.sun.net.httpserver.HttpServer;
import org.freakz.common.model.botconfig.WhatsAppConfig;
import org.freakz.common.model.feed.Message;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppCapturedReplyTest {
  @Test
  void textReplyCarriesOriginalSenderAndDisablesPreviewsEvenOnFallback() throws Exception {
    var mapper = JsonMapper.builder().build();
    var requests = new ArrayList<String>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/send", exchange -> {
      requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      // Simulate an unavailable quoted message, then accept a plain link fallback.
      exchange.sendResponseHeaders(requests.size() == 1 ? 400 : 200, -1);
      exchange.close();
    });
    server.start();
    try {
      var connection = new WhatsAppConnection(null);
      connection.applyConfig(WhatsAppConfig.builder()
          .sendBaseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());
      connection.sendMessageTo(Message.builder().id("group@g.us").message("https://bot.test/m/abc12")
          .replyToMessageId("original-id").replyToSenderId("sender@s.whatsapp.net")
          .suppressLinkPreviews(true).build());
      assertThat(requests).hasSize(2);
      var quoted = mapper.readTree(requests.get(0));
      assertThat(quoted.path("replyTo").asText()).isEqualTo("original-id");
      assertThat(quoted.path("replyToSender").asText()).isEqualTo("sender@s.whatsapp.net");
      for (String request : requests) {
        var payload = mapper.readTree(request);
        assertThat(payload.path("message").asText()).isEqualTo("https://bot.test/m/abc12");
        assertThat(payload.path("suppressLinkPreviews").asBoolean()).isTrue();
        assertThat(payload.has("media")).isFalse();
      }
      assertThat(mapper.readTree(requests.get(1)).has("replyTo")).isFalse();
    } finally {
      server.stop(0);
    }
  }
}
