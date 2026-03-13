package org.freakz.engine.dto;

import org.freakz.common.model.connectionmanager.BotConnectionResponse;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.Map;

public class ConnectionsResponse extends ServiceResponse {

  private Map<Integer, BotConnectionResponse> connectionMap;

  public ConnectionsResponse() {
  }

  public ConnectionsResponse(Map<Integer, BotConnectionResponse> connectionMap) {
    this.connectionMap = connectionMap;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Map<Integer, BotConnectionResponse> getConnectionMap() {
    return connectionMap;
  }

  public void setConnectionMap(Map<Integer, BotConnectionResponse> connectionMap) {
    this.connectionMap = connectionMap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConnectionsResponse that = (ConnectionsResponse) o;

    return connectionMap != null ? connectionMap.equals(that.connectionMap) : that.connectionMap == null;
  }

  @Override
  public int hashCode() {
    return connectionMap != null ? connectionMap.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "ConnectionsResponse{" +
        "connectionMap=" + connectionMap +
        '}';
  }

  public static class Builder {
    private Map<Integer, BotConnectionResponse> connectionMap;

    Builder() {
    }

    public Builder connectionMap(Map<Integer, BotConnectionResponse> connectionMap) {
      this.connectionMap = connectionMap;
      return this;
    }

    public ConnectionsResponse build() {
      return new ConnectionsResponse(connectionMap);
    }
  }
}
