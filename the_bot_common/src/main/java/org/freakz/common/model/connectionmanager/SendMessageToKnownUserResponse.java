package org.freakz.common.model.connectionmanager;

import java.util.List;

public class SendMessageToKnownUserResponse {

  private String status;
  private String sentTo;
  private String message;
  private KnownUserTargetResponse selectedTarget;
  private List<KnownUserTargetResponse> candidateTargets;

  public SendMessageToKnownUserResponse() {
  }

  public SendMessageToKnownUserResponse(
      String status,
      String sentTo,
      String message,
      KnownUserTargetResponse selectedTarget,
      List<KnownUserTargetResponse> candidateTargets) {
    this.status = status;
    this.sentTo = sentTo;
    this.message = message;
    this.selectedTarget = selectedTarget;
    this.candidateTargets = candidateTargets;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSentTo() {
    return sentTo;
  }

  public void setSentTo(String sentTo) {
    this.sentTo = sentTo;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public KnownUserTargetResponse getSelectedTarget() {
    return selectedTarget;
  }

  public void setSelectedTarget(KnownUserTargetResponse selectedTarget) {
    this.selectedTarget = selectedTarget;
  }

  public List<KnownUserTargetResponse> getCandidateTargets() {
    return candidateTargets;
  }

  public void setCandidateTargets(List<KnownUserTargetResponse> candidateTargets) {
    this.candidateTargets = candidateTargets;
  }
}
