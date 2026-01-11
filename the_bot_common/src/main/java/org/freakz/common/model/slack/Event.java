package org.freakz.common.model.slack;

import java.util.List;
import java.util.Objects;

public class Event {
  private String user;
  private String type;
  private String ts;
  private String clientMsgId;
  private String text;
  private String team;
  private List<Block> blocks;
  private String channel;
  private String eventTs;
  private String channelType;

  public Event() {
  }

  public Event(String user, String type, String ts, String clientMsgId, String text, String team, List<Block> blocks, String channel, String eventTs, String channelType) {
    this.user = user;
    this.type = type;
    this.ts = ts;
    this.clientMsgId = clientMsgId;
    this.text = text;
    this.team = team;
    this.blocks = blocks;
    this.channel = channel;
    this.eventTs = eventTs;
    this.channelType = channelType;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTs() {
    return ts;
  }

  public void setTs(String ts) {
    this.ts = ts;
  }

  public String getClientMsgId() {
    return clientMsgId;
  }

  public void setClientMsgId(String clientMsgId) {
    this.clientMsgId = clientMsgId;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getTeam() {
    return team;
  }

  public void setTeam(String team) {
    this.team = team;
  }

  public List<Block> getBlocks() {
    return blocks;
  }

  public void setBlocks(List<Block> blocks) {
    this.blocks = blocks;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getEventTs() {
    return eventTs;
  }

  public void setEventTs(String eventTs) {
    this.eventTs = eventTs;
  }

  public String getChannelType() {
    return channelType;
  }

  public void setChannelType(String channelType) {
    this.channelType = channelType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Event event = (Event) o;
    return Objects.equals(user, event.user) && Objects.equals(type, event.type) && Objects.equals(ts, event.ts) && Objects.equals(clientMsgId, event.clientMsgId) && Objects.equals(text, event.text) && Objects.equals(team, event.team) && Objects.equals(blocks, event.blocks) && Objects.equals(channel, event.channel) && Objects.equals(eventTs, event.eventTs) && Objects.equals(channelType, event.channelType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, type, ts, clientMsgId, text, team, blocks, channel, eventTs, channelType);
  }

  @Override
  public String toString() {
    return "Event{" +
        "user='" + user + '\'' +
        ", type='" + type + '\'' +
        ", ts='" + ts + '\'' +
        ", clientMsgId='" + clientMsgId + '\'' +
        ", text='" + text + '\'' +
        ", team='" + team + '\'' +
        ", blocks=" + blocks +
        ", channel='" + channel + '\'' +
        ", eventTs='" + eventTs + '\'' +
        ", channelType='" + channelType + '\'' +
        '}';
  }
}
