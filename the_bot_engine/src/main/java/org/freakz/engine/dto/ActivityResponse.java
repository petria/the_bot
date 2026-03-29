package org.freakz.engine.dto;

import org.freakz.common.model.connectionmanager.ChannelActivityResponse;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;
import java.util.Objects;

public class ActivityResponse extends ServiceResponse {

  private List<ChannelActivityResponse> channels;

  public ActivityResponse() {
  }

  public ActivityResponse(List<ChannelActivityResponse> channels) {
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
    ActivityResponse that = (ActivityResponse) o;
    return Objects.equals(channels, that.channels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channels);
  }

  public static class Builder {
    private List<ChannelActivityResponse> channels;

    public Builder channels(List<ChannelActivityResponse> channels) {
      this.channels = channels;
      return this;
    }

    public ActivityResponse build() {
      return new ActivityResponse(channels);
    }
  }
}
