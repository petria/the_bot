package org.freakz.engine.dto.stats;

import org.freakz.engine.services.api.ServiceResponse;

import java.util.Map;

public class TopStatsResponse extends ServiceResponse {

  private Map<String, StatsNode> nodeMap;

  public TopStatsResponse() {
  }

  public TopStatsResponse(Map<String, StatsNode> nodeMap) {
    this.nodeMap = nodeMap;
  }

  public Map<String, StatsNode> getNodeMap() {
    return nodeMap;
  }

  public void setNodeMap(Map<String, StatsNode> nodeMap) {
    this.nodeMap = nodeMap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TopStatsResponse that = (TopStatsResponse) o;

    return nodeMap != null ? nodeMap.equals(that.nodeMap) : that.nodeMap == null;
  }

  @Override
  public int hashCode() {
    return nodeMap != null ? nodeMap.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "TopStatsResponse{" +
        "nodeMap=" + nodeMap +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Map<String, StatsNode> nodeMap;

    Builder() {
    }

    public Builder nodeMap(Map<String, StatsNode> nodeMap) {
      this.nodeMap = nodeMap;
      return this;
    }

    public TopStatsResponse build() {
      return new TopStatsResponse(nodeMap);
    }
  }
}
