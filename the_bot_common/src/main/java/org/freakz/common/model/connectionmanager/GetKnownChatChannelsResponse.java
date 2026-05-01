package org.freakz.common.model.connectionmanager;

import java.util.List;

public class GetKnownChatChannelsResponse {

  private List<KnownChatChannelResponse> channels;

  public GetKnownChatChannelsResponse() {
  }

  public GetKnownChatChannelsResponse(List<KnownChatChannelResponse> channels) {
    this.channels = channels;
  }

  public List<KnownChatChannelResponse> getChannels() {
    return channels;
  }

  public void setChannels(List<KnownChatChannelResponse> channels) {
    this.channels = channels;
  }
}
