package org.freakz.common.model.botconfig;

import java.util.List;
import java.util.Objects;

public class IrcServerConfig {

  private String name;
  private IrcNetwork ircNetwork;
  private List<Channel> channelList;
  private boolean connectStartup;

  public IrcServerConfig() {
  }

  public IrcServerConfig(String name, IrcNetwork ircNetwork, List<Channel> channelList, boolean connectStartup) {
    this.name = name;
    this.ircNetwork = ircNetwork;
    this.channelList = channelList;
    this.connectStartup = connectStartup;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public IrcNetwork getIrcNetwork() {
    return ircNetwork;
  }

  public void setIrcNetwork(IrcNetwork ircNetwork) {
    this.ircNetwork = ircNetwork;
  }

  public List<Channel> getChannelList() {
    return channelList;
  }

  public void setChannelList(List<Channel> channelList) {
    this.channelList = channelList;
  }

  public boolean isConnectStartup() {
    return connectStartup;
  }

  public void setConnectStartup(boolean connectStartup) {
    this.connectStartup = connectStartup;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IrcServerConfig that = (IrcServerConfig) o;
    return connectStartup == that.connectStartup && Objects.equals(name, that.name) && Objects.equals(ircNetwork, that.ircNetwork) && Objects.equals(channelList, that.channelList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, ircNetwork, channelList, connectStartup);
  }

  @Override
  public String toString() {
    return "IrcServerConfig{" +
        "name='" + name + '\'' +
        ", ircNetwork=" + ircNetwork +
        ", channelList=" + channelList +
        ", connectStartup=" + connectStartup +
        '}';
  }

  public static class Builder {
    private String name;
    private IrcNetwork ircNetwork;
    private List<Channel> channelList;
    private boolean connectStartup;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder ircNetwork(IrcNetwork ircNetwork) {
      this.ircNetwork = ircNetwork;
      return this;
    }

    public Builder channelList(List<Channel> channelList) {
      this.channelList = channelList;
      return this;
    }

    public Builder connectStartup(boolean connectStartup) {
      this.connectStartup = connectStartup;
      return this;
    }

    public IrcServerConfig build() {
      return new IrcServerConfig(name, ircNetwork, channelList, connectStartup);
    }
  }
}
