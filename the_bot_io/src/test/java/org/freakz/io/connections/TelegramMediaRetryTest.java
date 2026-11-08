package org.freakz.io.connections;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.TelegramConfig;
import org.freakz.common.model.feed.Message;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import tools.jackson.databind.json.JsonMapper;

import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramMediaRetryTest {
  private final Channel channel = Channel.builder().id("-123").name("Test group")
      .echoToAlias("TELEGRAM-TEST").captureImages(true).showCapturedUrl(true).build();
  private final List<Message> replies = new ArrayList<>();
  private final BotConnection connection = new BotConnection(BotConnectionType.TELEGRAM_CONNECTION) {
    @Override public void sendMessageTo(Message message) { replies.add(message); }
  };
  private final RecordingCapture capture = new RecordingCapture();

  @Test
  void retriesTimedOutFileLookupWithoutLosingOriginalReplyContext() {
    TestBot bot = bot(List.of(timeout()));
    bot.onUpdateReceived(photoUpdate());
    assertThat(bot.fileCalls).isEqualTo(2);
    assertThat(capture.calls).isEqualTo(1);
    assertThat(capture.url).endsWith("/photos/test.jpg");
    assertThat(capture.replyContext).isEqualTo(new MediaCaptureService.ReplyContext("-123", "42", "7", "123"));
    assertThat(replies).isEmpty();
  }

  @Test
  void retriesTelegramServerErrors() {
    TestBot bot = bot(List.of(apiError(502)));
    bot.onUpdateReceived(photoUpdate());
    assertThat(bot.fileCalls).isEqualTo(2);
    assertThat(capture.calls).isEqualTo(1);
  }

  @Test
  void exhaustedFileLookupNotifiesOriginalMessageInsteadOfCapturingNullUrl() {
    TestBot bot = bot(List.of(timeout(), timeout(), timeout()));
    bot.onUpdateReceived(photoUpdate());
    assertThat(bot.fileCalls).isEqualTo(3);
    assertThat(capture.calls).isZero();
    assertThat(replies).hasSize(1);
    Message failure = replies.getFirst();
    assertThat(failure.getId()).isEqualTo("-123");
    assertThat(failure.getReplyToMessageId()).isEqualTo("42");
    assertThat(failure.getMessageThreadId()).isEqualTo("7");
    assertThat(failure.getMessage()).isEqualTo("Couldn't save this attachment. Please try again.");
    assertThat(failure.isSuppressLinkPreviews()).isTrue();
  }

  @Test
  void permanentErrorsDoNotRetry() {
    TestBot bot = bot(List.of(apiError(400)));
    bot.onUpdateReceived(photoUpdate());
    assertThat(bot.fileCalls).isEqualTo(1);
    assertThat(capture.calls).isZero();
    assertThat(replies).hasSize(1);
  }

  @Test
  void disabledCaptureDoesNotBlockHandlerWithUnneededFileLookup() {
    channel.setCaptureImages(false);
    TestBot bot = bot(List.of(timeout()));
    bot.onUpdateReceived(photoUpdate());
    assertThat(bot.fileCalls).isZero();
    assertThat(capture.calls).isZero();
    assertThat(replies).isEmpty();
  }

  @Test
  void failureNotificationRespectsSourceReplyToggle() {
    channel.setShowCapturedUrl(false);
    channel.setCaptureImageToAliases(List.of("IRC-TEST"));
    TestBot bot = bot(List.of(apiError(400)));
    bot.onUpdateReceived(photoUpdate());
    assertThat(capture.calls).isZero();
    assertThat(replies).isEmpty();
  }

  @Test
  void apiTimeoutBoundsPollingAndFileLookup() {
    TestBot bot = bot(List.of());
    assertThat(bot.getOptions().getGetUpdatesTimeout()).isEqualTo(10);
    assertThat(bot.getOptions().getRequestConfig()).isNotNull();
    assertThat(bot.getOptions().getRequestConfig().getSocketTimeout()).isEqualTo(15_000);
    assertThat(bot.getOptions().getRequestConfig().getConnectTimeout()).isEqualTo(5_000);
  }

  private TestBot bot(List<TelegramApiException> failures) {
    return new TestBot(new TelegramConfig("test_bot", "123:test-token", List.of(channel), true), failures);
  }

  private static TelegramApiException timeout() {
    return new TelegramApiException("Unable to execute getFile method", new SocketTimeoutException("Read timed out"));
  }

  private static TelegramApiRequestException apiError(int code) {
    return new TelegramApiRequestException("getFile failed") {
      @Override public Integer getErrorCode() { return code; }
    };
  }

  private static Update photoUpdate() {
    Chat chat = new Chat();
    chat.setId(-123L);
    chat.setType("supergroup");
    chat.setTitle("Test group");
    User user = new User();
    user.setId(123L);
    user.setUserName("Alice");
    PhotoSize photo = new PhotoSize();
    photo.setFileId("telegram-file-id");
    photo.setWidth(100);
    photo.setHeight(100);
    var message = new org.telegram.telegrambots.meta.api.objects.Message();
    message.setChat(chat);
    message.setFrom(user);
    message.setMessageId(42);
    message.setMessageThreadId(7);
    message.setPhoto(List.of(photo));
    Update update = new Update();
    update.setMessage(message);
    return update;
  }

  private class TestBot extends TelegramConnection.HokanTelegram {
    private final List<TelegramApiException> failures;
    private int fileCalls;
    TestBot(TelegramConfig config, List<TelegramApiException> failures) {
      super(new ConnectionManager(), "123:test-token", connection, null, capture, "test_bot", config);
      this.failures = failures;
    }
    @Override @SuppressWarnings("unchecked")
    public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) throws TelegramApiException {
      assertThat(method).isInstanceOf(GetFile.class);
      fileCalls++;
      if (fileCalls <= failures.size()) throw failures.get(fileCalls - 1);
      var file = new org.telegram.telegrambots.meta.api.objects.File();
      file.setFilePath("photos/test.jpg");
      return (T) file;
    }
  }

  private static class RecordingCapture extends MediaCaptureService {
    private int calls;
    private String url;
    private ReplyContext replyContext;
    RecordingCapture() { super(null, JsonMapper.builder().build()); }
    @Override public void captureAndSend(ConnectionManager manager, Channel channel, BotConnection connection,
        String protocol, String sender, String caption, String url, String contentType, String fileName,
        Map<String, String> headers, ReplyContext replyContext) {
      calls++;
      this.url = url;
      this.replyContext = replyContext;
    }
  }
}
