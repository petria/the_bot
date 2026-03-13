package org.freakz.common.model.users;

import java.util.Objects;

public class GetUsersRequest {

  private long timestamp;

  public GetUsersRequest() {
  }

  public GetUsersRequest(long timestamp) {
    this.timestamp = timestamp;
  }

  public static Builder builder() {
    return new Builder();
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GetUsersRequest that = (GetUsersRequest) o;
    return timestamp == that.timestamp;
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp);
  }

  @Override
  public String toString() {
    return "GetUsersRequest{" +
        "timestamp=" + timestamp +
        '}';
  }

  public static class Builder {
    private long timestamp;

    public Builder timestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public GetUsersRequest build() {
      return new GetUsersRequest(timestamp);
    }
  }
}
