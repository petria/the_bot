package org.freakz.common.model.connectionmanager;

import java.util.List;

public class GetKnownUserTargetsResponse {

  private List<KnownUserTargetResponse> targets;

  public GetKnownUserTargetsResponse() {
  }

  public GetKnownUserTargetsResponse(List<KnownUserTargetResponse> targets) {
    this.targets = targets;
  }

  public List<KnownUserTargetResponse> getTargets() {
    return targets;
  }

  public void setTargets(List<KnownUserTargetResponse> targets) {
    this.targets = targets;
  }
}
