package org.freakz.common.model.connectionmanager;

import java.util.List;

public record IrcModeStatesResponse(List<IrcModeStateResponse> modes) {
  public IrcModeStatesResponse {
    modes = modes == null ? List.of() : List.copyOf(modes);
  }
}
