package org.freakz.common.model.connectionmanager;

import java.util.List;
import java.util.Objects;

public class GetChannelActivityResponse {

  private List<ChannelActivityResponse> channels;

  public GetChannelActivityResponse() {
  }

  public GetChannelActivityResponse(List<ChannelActivityResponse> channels) {
    this.channels = channels;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<ChannelActivityResponse> getChannels() {
    return channels;
  }

  public void setChannels(List<ChannelActivityResponse> channels) {
    this.channels = channels;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GetChannelActivityResponse that = (GetChannelActivityResponse) o;
    return Objects.equals(channels, that.channels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channels);
  }

  @Override
  public String toString() {
    return "GetChannelActivityResponse{" +
        "channels=" + channels +
        '}';
  }

  public static class Builder {
    private List<ChannelActivityResponse> channels;

    public Builder channels(List<ChannelActivityResponse> channels) {
      this.channels = channels;
      return this;
    }

    public GetChannelActivityResponse build() {
      return new GetChannelActivityResponse(channels);
    }
  }
}
