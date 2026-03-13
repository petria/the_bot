package org.freakz.common.model.engine;

import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.users.User;

import java.util.Objects;

public class EngineRequest {

  private long timestamp;
  private String command;
  private String replyTo;
  private int fromConnectionId;

  private boolean isPrivateChannel;
  private Long fromChannelId;

  private String fromSenderId;
  private String fromSender;
  private boolean isFromAdmin;

  private String network;

  private String echoToAlias;

  private User user;
  private TheBotConfig botConfig;

  public EngineRequest() {
  }

  public EngineRequest(long timestamp, String command, String replyTo, int fromConnectionId, boolean isPrivateChannel, Long fromChannelId, String fromSenderId, String fromSender, boolean isFromAdmin, String network, String echoToAlias, User user, TheBotConfig botConfig) {
    this.timestamp = timestamp;
    this.command = command;
    this.replyTo = replyTo;
    this.fromConnectionId = fromConnectionId;
    this.isPrivateChannel = isPrivateChannel;
    this.fromChannelId = fromChannelId;
    this.fromSenderId = fromSenderId;
    this.fromSender = fromSender;
    this.isFromAdmin = isFromAdmin;
    this.network = network;
    this.echoToAlias = echoToAlias;
    this.user = user;
    this.botConfig = botConfig;
  }

  public static Builder builder() {
    return new Builder();
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public String getReplyTo() {
    return replyTo;
  }

  public void setReplyTo(String replyTo) {
    this.replyTo = replyTo;
  }

  public int getFromConnectionId() {
    return fromConnectionId;
  }

  public void setFromConnectionId(int fromConnectionId) {
    this.fromConnectionId = fromConnectionId;
  }

  public Long getFromChannelId() {
    return fromChannelId;
  }

  public void setFromChannelId(Long fromChannelId) {
    this.fromChannelId = fromChannelId;
  }

  public String getFromSenderId() {
    return fromSenderId;
  }

  public void setFromSenderId(String fromSenderId) {
    this.fromSenderId = fromSenderId;
  }

  public String getFromSender() {
    return fromSender;
  }

  public void setFromSender(String fromSender) {
    this.fromSender = fromSender;
  }

  public boolean isFromAdmin() {
    return isFromAdmin;
  }

  public void setFromAdmin(boolean fromAdmin) {
    isFromAdmin = fromAdmin;
  }

  public String getNetwork() {
    return network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  public String getEchoToAlias() {
    return echoToAlias;
  }

  public void setEchoToAlias(String echoToAlias) {
    this.echoToAlias = echoToAlias;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public TheBotConfig getBotConfig() {
    return botConfig;
  }

  public void setBotConfig(TheBotConfig botConfig) {
    this.botConfig = botConfig;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EngineRequest that = (EngineRequest) o;
    return timestamp == that.timestamp && fromConnectionId == that.fromConnectionId && isPrivateChannel == that.isPrivateChannel && isFromAdmin == that.isFromAdmin && Objects.equals(command, that.command) && Objects.equals(replyTo, that.replyTo) && Objects.equals(fromChannelId, that.fromChannelId) && Objects.equals(fromSenderId, that.fromSenderId) && Objects.equals(fromSender, that.fromSender) && Objects.equals(network, that.network) && Objects.equals(echoToAlias, that.echoToAlias) && Objects.equals(user, that.user) && Objects.equals(botConfig, that.botConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, command, replyTo, fromConnectionId, isPrivateChannel, fromChannelId, fromSenderId, fromSender, isFromAdmin, network, echoToAlias, user, botConfig);
  }

  @Override
  public String toString() {
    return "EngineRequest{" +
        "timestamp=" + timestamp +
        ", command='" + command + '\'' +
        ", replyTo='" + replyTo + '\'' +
        ", fromConnectionId=" + fromConnectionId +
        ", isPrivateChannel=" + isPrivateChannel +
        ", fromChannelId=" + fromChannelId +
        ", fromSenderId='" + fromSenderId + '\'' +
        ", fromSender='" + fromSender + '\'' +
        ", isFromAdmin=" + isFromAdmin +
        ", network='" + network + '\'' +
        ", echoToAlias='" + echoToAlias + '\'' +
        ", user=" + user +
        ", botConfig=" + botConfig +
        '}';
  }

  public String getMessage() {
    return command;
  }

  public boolean isPrivateChannel() {
    return isPrivateChannel;
  }

  public void setPrivateChannel(boolean privateChannel) {
    isPrivateChannel = privateChannel;
  }

  public static class Builder {
    private long timestamp;
    private String command;
    private String replyTo;
    private int fromConnectionId;
    private boolean isPrivateChannel;
    private Long fromChannelId;
    private String fromSenderId;
    private String fromSender;
    private boolean isFromAdmin;
    private String network;
    private String echoToAlias;
    private User user;
    private TheBotConfig botConfig;

    public Builder timestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder command(String command) {
      this.command = command;
      return this;
    }

    public Builder replyTo(String replyTo) {
      this.replyTo = replyTo;
      return this;
    }

    public Builder fromConnectionId(int fromConnectionId) {
      this.fromConnectionId = fromConnectionId;
      return this;
    }

    public Builder isPrivateChannel(boolean isPrivateChannel) {
      this.isPrivateChannel = isPrivateChannel;
      return this;
    }

    public Builder fromChannelId(Long fromChannelId) {
      this.fromChannelId = fromChannelId;
      return this;
    }

    public Builder fromSenderId(String fromSenderId) {
      this.fromSenderId = fromSenderId;
      return this;
    }

    public Builder fromSender(String fromSender) {
      this.fromSender = fromSender;
      return this;
    }

    public Builder isFromAdmin(boolean isFromAdmin) {
      this.isFromAdmin = isFromAdmin;
      return this;
    }

    public Builder network(String network) {
      this.network = network;
      return this;
    }

    public Builder echoToAlias(String echoToAlias) {
      this.echoToAlias = echoToAlias;
      return this;
    }

    public Builder user(User user) {
      this.user = user;
      return this;
    }

    public Builder botConfig(TheBotConfig botConfig) {
      this.botConfig = botConfig;
      return this;
    }

    public EngineRequest build() {
      return new EngineRequest(timestamp, command, replyTo, fromConnectionId, isPrivateChannel, fromChannelId, fromSenderId, fromSender, isFromAdmin, network, echoToAlias, user, botConfig);
    }
  }
}
