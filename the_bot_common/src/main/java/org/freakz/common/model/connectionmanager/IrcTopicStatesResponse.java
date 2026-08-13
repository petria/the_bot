package org.freakz.common.model.connectionmanager;

import java.util.List;

public record IrcTopicStatesResponse(List<IrcTopicStateResponse> topics) {
  public IrcTopicStatesResponse {
    topics = topics == null ? List.of() : List.copyOf(topics);
  }
}
