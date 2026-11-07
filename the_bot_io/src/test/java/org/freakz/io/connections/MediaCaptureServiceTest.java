package org.freakz.io.connections;

import com.sun.net.httpserver.HttpServer;
import org.freakz.common.media.MediaStore;
import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.engine.system.MediaStorageSettingsResponse;
import org.freakz.common.model.feed.Message;
import org.freakz.common.spring.rest.RestEngineClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MediaCaptureServiceTest {
  @TempDir Path storage;
  private final JsonMapper mapper = JsonMapper.builder().build();
  private final RecordingManager manager = new RecordingManager();
  private final RecordingConnection connection = new RecordingConnection();
  private HttpServer server;
  private MediaCaptureService service;
  private Channel channel;
  private String downloadUrl;
  private boolean enabled = true;
  private int requests;
  private int status = 200;
  private byte[] body = new byte[] {1, 2, 3};

  @BeforeEach
  void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/image", exchange -> {
      requests++;
      exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
      exchange.sendResponseHeaders(status, body.length);
      try (var output = exchange.getResponseBody()) {
        output.write(body);
      }
    });
    server.start();
    downloadUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/image";
    RestEngineClient engine = new RestEngineClient(new RestTemplate(), "http://unused") {
      @Override
      public ResponseEntity<MediaStorageSettingsResponse> getMediaStorageSettings() {
        return ResponseEntity.ok(new MediaStorageSettingsResponse(enabled, storage.toString(),
            "https://bot.test/media", 1, 30, true, true, null));
      }
    };
    service = new MediaCaptureService(engine, mapper);
    channel = Channel.builder().id("-123").name("Test group").echoToAlias("TELEGRAM-TEST")
        .captureImages(true).showCapturedUrl(true).build();
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void storesOnceAndRepliesToSourceWithoutForwardingTargets() throws Exception {
    capture();
    assertThat(connection.messages).hasSize(1);
    Message reply = connection.messages.getFirst();
    assertThat(reply.getId()).isEqualTo("-123");
    assertThat(reply.getTarget()).isEqualTo("Test group");
    MediaStore store = new MediaStore(storage, mapper);
    var records = store.listActive();
    assertThat(records).hasSize(1);
    assertThat(reply.getMessage()).isEqualTo("Alice: caption [image: https://bot.test/m/" + records.getFirst().shortCode() + "]");
    assertThat(Files.readAllBytes(store.readPublicByShortCode(records.getFirst().shortCode()).orElseThrow().file()))
        .containsExactly(body);
    assertThat(manager.aliases).isEmpty();
    assertThat(requests).isEqualTo(1);
  }

  @Test
  void forwardsWithoutSourceReplyForOldConfigurations() throws Exception {
    channel.setShowCapturedUrl(null);
    channel.setCaptureImageToAliases(List.of("IRC-TARGET"));
    capture();
    assertThat(connection.messages).isEmpty();
    assertThat(manager.aliases).containsExactly("IRC-TARGET");
    assertThat(new MediaStore(storage, mapper).listActive()).hasSize(1);
  }

  @Test
  void deduplicatesSourceAndForwardingAliases() throws Exception {
    channel.setCaptureImageToAliases(List.of(" telegram-test ", "IRC-TARGET", " irc-target ", " "));
    capture();
    assertThat(connection.messages).hasSize(1);
    assertThat(manager.aliases).containsExactly("IRC-TARGET");
    assertThat(new MediaStore(storage, mapper).listActive()).hasSize(1);
  }

  @Test
  void skipsCaptureWithNeitherSourceReplyNorValidTargets() {
    channel.setShowCapturedUrl(false);
    channel.setCaptureImageToAliases(List.of(" "));
    capture();
    assertThat(requests).isZero();
    assertThat(connection.messages).isEmpty();
  }

  @Test
  void mediaToggleGatesSourceReply() {
    channel.setCaptureImages(false);
    capture();
    assertThat(requests).isZero();
    assertThat(connection.messages).isEmpty();
  }

  @Test
  void globalStorageToggleGatesSourceReply() {
    enabled = false;
    capture();
    assertThat(requests).isZero();
    assertThat(connection.messages).isEmpty();
  }

  @Test
  void failedDownloadProducesNoLinksOrStoredMedia() throws Exception {
    status = 404;
    capture();
    assertNoCapture();
  }

  @Test
  void oversizedDownloadProducesNoLinksOrStoredMedia() throws Exception {
    body = new byte[1024 * 1024 + 1];
    capture();
    assertNoCapture();
  }

  @Test
  void failedStorageProducesNoLinks() throws Exception {
    Files.writeString(storage.resolve("files"), "blocks the media directory");
    capture();
    assertThat(connection.messages).isEmpty();
    assertThat(manager.aliases).isEmpty();
  }

  @Test
  void sourceDeliveryFailureDoesNotBlockForwarding() {
    connection.fail = true;
    channel.setCaptureImageToAliases(List.of("IRC-TARGET"));
    capture();
    assertThat(manager.aliases).containsExactly("IRC-TARGET");
  }

  @Test
  void forwardingFailureDoesNotBlockSourceOrOtherTargets() {
    manager.failAlias = "A-FAIL";
    channel.setCaptureImageToAliases(List.of("A-FAIL", "IRC-TARGET"));
    capture();
    assertThat(connection.messages).hasSize(1);
    assertThat(manager.aliases).containsExactly("IRC-TARGET");
  }

  private void assertNoCapture() throws Exception {
    assertThat(connection.messages).isEmpty();
    assertThat(manager.aliases).isEmpty();
    assertThat(new MediaStore(storage, mapper).listActive()).isEmpty();
  }

  private void capture() {
    service.captureAndSend(manager, channel, connection, "Telegram", "Alice", "caption",
        downloadUrl, "image/jpeg", "photo.jpg");
  }

  private static class RecordingConnection extends BotConnection {
    private final List<Message> messages = new ArrayList<>();
    private boolean fail;
    @Override public void sendMessageTo(Message message) {
      if (fail) throw new IllegalStateException("Source send failed");
      messages.add(message);
    }
  }

  private static class RecordingManager extends ConnectionManager {
    private final List<String> aliases = new ArrayList<>();
    private String failAlias;
    @Override public void sendMessageByEchoToAlias(String message, String alias) {
      if (alias.equals(failAlias)) throw new IllegalStateException("Forward send failed");
      aliases.add(alias);
    }
  }
}
