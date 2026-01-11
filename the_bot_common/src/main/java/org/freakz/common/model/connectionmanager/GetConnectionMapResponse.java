package org.freakz.common.model.connectionmanager;

import java.util.Map;
import java.util.Objects;

public class GetConnectionMapResponse {

  private Map<Integer, BotConnectionResponse> connectionMap;

  public GetConnectionMapResponse() {
  }

  public GetConnectionMapResponse(Map<Integer, BotConnectionResponse> connectionMap) {
    this.connectionMap = connectionMap;
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
    GetConnectionMapResponse that = (GetConnectionMapResponse) o;
    return Objects.equals(connectionMap, that.connectionMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionMap);
  }

  @Override
  public String toString() {
    return "GetConnectionMapResponse{" +
        "connectionMap=" + connectionMap +
        '}';
  }
}
