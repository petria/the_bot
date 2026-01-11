package org.freakz.engine.commands.util;

import org.freakz.common.model.users.User;

public class UserAndReply {

  private User user;

  private String replyMessage;

  public UserAndReply() {
  }

  public UserAndReply(User user, String replyMessage) {
    this.user = user;
    this.replyMessage = replyMessage;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getReplyMessage() {
    return replyMessage;
  }

  public void setReplyMessage(String replyMessage) {
    this.replyMessage = replyMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserAndReply that = (UserAndReply) o;

    if (user != null ? !user.equals(that.user) : that.user != null) return false;
    return replyMessage != null ? replyMessage.equals(that.replyMessage) : that.replyMessage == null;
  }

  @Override
  public int hashCode() {
    int result = user != null ? user.hashCode() : 0;
    result = 31 * result + (replyMessage != null ? replyMessage.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "UserAndReply{" +
        "user=" + user +
        ", replyMessage='" + replyMessage + '\'' +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private User user;
    private String replyMessage;

    Builder() {
    }

    public Builder user(User user) {
      this.user = user;
      return this;
    }

    public Builder replyMessage(String replyMessage) {
      this.replyMessage = replyMessage;
      return this;
    }

    public UserAndReply build() {
      return new UserAndReply(user, replyMessage);
    }

    @Override
    public String toString() {
      return "Builder{" +
          "user=" + user +
          ", replyMessage='" + replyMessage + '\'' +
          '}';
    }
  }
}

