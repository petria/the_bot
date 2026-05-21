package org.freakz.common.model.engine.commands;

import java.util.ArrayList;
import java.util.List;

public class GetCommandsResponse {

  private List<CommandProviderInfo> providers = new ArrayList<>();

  public GetCommandsResponse() {
  }

  public GetCommandsResponse(List<CommandProviderInfo> providers) {
    this.providers = providers == null ? new ArrayList<>() : new ArrayList<>(providers);
  }

  public List<CommandProviderInfo> getProviders() {
    return providers;
  }

  public void setProviders(List<CommandProviderInfo> providers) {
    this.providers = providers == null ? new ArrayList<>() : new ArrayList<>(providers);
  }
}
