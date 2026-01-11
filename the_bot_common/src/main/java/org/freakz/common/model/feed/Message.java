package org.freakz.common.model.feed;

import java.time.LocalDateTime;
import java.util.Objects;

public class Message {

  private String id;
  private MessageSource messageSource;
  private long timestamp;
  private long requestTimestamp;

  private LocalDateTime time;
  private String sender;
  private String target;
  private String message;

  public Message() {
  }

  public Message(String id, MessageSource messageSource, long timestamp, long requestTimestamp, LocalDateTime time, String sender, String target, String message) {
    this.id = id;
    this.messageSource = messageSource;
    this.timestamp = timestamp;
    this.requestTimestamp = requestTimestamp;
    this.time = time;
    this.sender = sender;
    this.target = target;
    this.message = message;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public MessageSource getMessageSource() {
    return messageSource;
  }

  public void setMessageSource(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public long getRequestTimestamp() {
    return requestTimestamp;
  }

  public void setRequestTimestamp(long requestTimestamp) {
    this.requestTimestamp = requestTimestamp;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public void setTime(LocalDateTime time) {
    this.time = time;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Message message1 = (Message) o;
    return timestamp == message1.timestamp && requestTimestamp == message1.requestTimestamp && Objects.equals(id, message1.id) && Objects.equals(messageSource, message1.messageSource) && Objects.equals(time, message1.time) && Objects.equals(sender, message1.sender) && Objects.equals(target, message1.target) && Objects.equals(message, message1.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, messageSource, timestamp, requestTimestamp, time, sender, target, message);
  }

  @Override
  public String toString() {
    return "Message{" +
        "id='" + id + '\'' +
        ", messageSource=" + messageSource +
        ", timestamp=" + timestamp +
        ", requestTimestamp=" + requestTimestamp +
        ", time=" + time +
        ", sender='" + sender + '\'' +
        ", target='" + target + '\'' +
        ", message='" + message + '\'' +
        '}';
  }

  public static class Builder {
    private String id;
    private MessageSource messageSource;
    private long timestamp;
    private long requestTimestamp;
    private LocalDateTime time;
    private String sender;
    private String target;
    private String message;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder messageSource(MessageSource messageSource) {
      this.messageSource = messageSource;
      return this;
    }

    public Builder timestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder requestTimestamp(long requestTimestamp) {
      this.requestTimestamp = requestTimestamp;
      return this;
    }

    public Builder time(LocalDateTime time) {
      this.time = time;
      return this;
    }

    public Builder sender(String sender) {
      this.sender = sender;
      return this;
    }

    public Builder target(String target) {
      this.target = target;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Message build() {
      return new Message(id, messageSource, timestamp, requestTimestamp, time, sender, target, message);
    }
  }
}
