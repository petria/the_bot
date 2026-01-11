package org.freakz.common.model.slack;

import java.util.Objects;

public class UrlVerificationResponse {

  private String challenge;

  public String getChallenge() {
    return challenge;
  }

  public void setChallenge(String challenge) {
    this.challenge = challenge;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UrlVerificationResponse that = (UrlVerificationResponse) o;
    return Objects.equals(challenge, that.challenge);
  }

  @Override
  public int hashCode() {
    return Objects.hash(challenge);
  }

  @Override
  public String toString() {
    return "UrlVerificationResponse{" +
        "challenge='" + challenge + '\'' +
        '}';
  }
}
