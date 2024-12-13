package org.freakz.io.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.feed.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageFeederService {

  private final List<Message> feed = new ArrayList<>();

  public MessageFeederService() {}

  public List<Message> getMessagesSinceTimestamp(long timestamp) {
    List<Message> collect =
        this.feed.stream().filter(m -> m.getTimestamp() > timestamp).collect(Collectors.toList());
    return collect;
  }

  public int insertMessage(Message message) {
    message.setTimestamp(System.currentTimeMillis());
    //        message.setTime(LocalDateTime.now());
    message.setId("" + this.feed.size());
    this.feed.add(message);
    return this.feed.size();
  }

  public List<Message> getLastMessages(long max) {
    List<Message> list = new ArrayList<>();
    int count = 0;
    for (Message message : this.feed) {
      list.add(message);
      count++;
      if (count == max) {
        break;
      }
    }
    return list;
  }

  public List<Message> getMessagesAfterId(long id) {
    List<Message> list = new ArrayList<>();
    for (Message message : this.feed) {
      int msgId = Integer.parseInt(message.getId());
      if (msgId > id) {
        list.add(message);
      }
    }
    return list;
  }

  public List<Message> getMessagesForDay(LocalDate day) {
    String dayStr = day.format(DateTimeFormatter.ISO_DATE);
    List<Message> list = new ArrayList<>();
    for (Message message : this.feed) {
      LocalDate localDate = message.getTime().toLocalDate();
      String dayStr2 = localDate.format(DateTimeFormatter.ISO_DATE);
      if (dayStr2.equals(dayStr)) {
        list.add(message);
      }
    }
    return list;
  }

  public int getCount() {
    return this.feed.size();
  }
}
