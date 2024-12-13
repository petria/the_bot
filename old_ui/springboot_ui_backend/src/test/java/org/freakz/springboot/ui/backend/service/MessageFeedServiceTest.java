package org.freakz.springboot.ui.backend.service;

import org.freakz.common.model.feed.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

public class MessageFeedServiceTest {

  @Test
  public void testGetMessagesForDay() {
    MessageFeederService sut = new MessageFeederService();

    // --- day 1
    Message msg =
        Message.builder()
            .time(LocalDateTime.of(2022, 10, 10, 10, 10, 0))
            .message("msg 10 10 10")
            .build();
    sut.insertMessage(msg);

    msg =
        Message.builder()
            .time(LocalDateTime.of(2022, 10, 10, 11, 11, 0))
            .message("msg 10 11 11")
            .build();
    sut.insertMessage(msg);

    msg =
        Message.builder()
            .time(LocalDateTime.of(2022, 10, 10, 12, 12, 0))
            .message("msg 10 12 12")
            .build();
    sut.insertMessage(msg);
    // --- day 2
    msg =
        Message.builder()
            .time(LocalDateTime.of(2022, 10, 11, 12, 12, 0))
            .message("msg 11 12 12")
            .build();
    sut.insertMessage(msg);

    msg =
        Message.builder()
            .time(LocalDateTime.of(2022, 10, 11, 13, 13, 0))
            .message("msg 11 13 13")
            .build();
    sut.insertMessage(msg);

    List<Message> forDay1 =
        sut.getMessagesForDay(LocalDateTime.of(2022, 10, 10, 11, 11, 0).toLocalDate());
    List<Message> forDay2 =
        sut.getMessagesForDay(LocalDateTime.of(2022, 10, 11, 13, 13, 0).toLocalDate());

    Assertions.assertEquals(3, forDay1.size());
    Assertions.assertEquals(2, forDay2.size());
  }
}
