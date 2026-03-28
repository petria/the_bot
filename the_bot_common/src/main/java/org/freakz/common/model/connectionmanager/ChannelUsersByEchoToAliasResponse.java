package org.freakz.common.model.connectionmanager;

import java.util.List;
import java.util.Objects;

public class ChannelUsersByEchoToAliasResponse {

  private List<ChannelUser> channelUsers;

  public ChannelUsersByEchoToAliasResponse() {
  }

  public ChannelUsersByEchoToAliasResponse(List<ChannelUser> channelUsers) {
    this.channelUsers = channelUsers;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<ChannelUser> getChannelUsers() {
    return channelUsers;
  }

  public void setChannelUsers(List<ChannelUser> channelUsers) {
    this.channelUsers = channelUsers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChannelUsersByEchoToAliasResponse that = (ChannelUsersByEchoToAliasResponse) o;
    return Objects.equals(channelUsers, that.channelUsers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channelUsers);
  }

  @Override
  public String toString() {
    return "ChannelUsersByEchoToAliasResponse{" +
        "channelUsers=" + channelUsers +
        '}';
  }

  public static class Builder {
    private List<ChannelUser> channelUsers;

    public Builder channelUsers(List<ChannelUser> channelUsers) {
      this.channelUsers = channelUsers;
      return this;
    }

    public ChannelUsersByEchoToAliasResponse build() {
      return new ChannelUsersByEchoToAliasResponse(channelUsers);
    }
  }
}
