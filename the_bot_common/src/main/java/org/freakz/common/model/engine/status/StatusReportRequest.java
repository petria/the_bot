package org.freakz.common.model.engine.status;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

public class StatusReportRequest {

  private long timestamp;
  private long uptimeStart;
  private String name;
  private String hostname;
  private String user;

  private ConcurrentMap<String, Integer> httpMethodCallMap;
  private Map<String, ChannelMessageCounters> channelMessageCountersMap;

  public StatusReportRequest() {
  }

  public StatusReportRequest(long timestamp, long uptimeStart, String name, String hostname, String user, ConcurrentMap<String, Integer> httpMethodCallMap, Map<String, ChannelMessageCounters> channelMessageCountersMap) {
    this.timestamp = timestamp;
    this.uptimeStart = uptimeStart;
    this.name = name;
    this.hostname = hostname;
    this.user = user;
    this.httpMethodCallMap = httpMethodCallMap;
    this.channelMessageCountersMap = channelMessageCountersMap;
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

  public long getUptimeStart() {
    return uptimeStart;
  }

  public void setUptimeStart(long uptimeStart) {
    this.uptimeStart = uptimeStart;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public ConcurrentMap<String, Integer> getHttpMethodCallMap() {
    return httpMethodCallMap;
  }

  public void setHttpMethodCallMap(ConcurrentMap<String, Integer> httpMethodCallMap) {
    this.httpMethodCallMap = httpMethodCallMap;
  }

  public Map<String, ChannelMessageCounters> getChannelMessageCountersMap() {
    return channelMessageCountersMap;
  }

  public void setChannelMessageCountersMap(Map<String, ChannelMessageCounters> channelMessageCountersMap) {
    this.channelMessageCountersMap = channelMessageCountersMap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StatusReportRequest that = (StatusReportRequest) o;
    return timestamp == that.timestamp && uptimeStart == that.uptimeStart && Objects.equals(name, that.name) && Objects.equals(hostname, that.hostname) && Objects.equals(user, that.user) && Objects.equals(httpMethodCallMap, that.httpMethodCallMap) && Objects.equals(channelMessageCountersMap, that.channelMessageCountersMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, uptimeStart, name, hostname, user, httpMethodCallMap, channelMessageCountersMap);
  }

  @Override
  public String toString() {
    return "StatusReportRequest{" +
        "timestamp=" + timestamp +
        ", uptimeStart=" + uptimeStart +
        ", name='" + name + '\'' +
        ", hostname='" + hostname + '\'' +
        ", user='" + user + '\'' +
        ", httpMethodCallMap=" + httpMethodCallMap +
        ", channelMessageCountersMap=" + channelMessageCountersMap +
        '}';
  }

  public static class Builder {
    private long timestamp;
    private long uptimeStart;
    private String name;
    private String hostname;
    private String user;
    private ConcurrentMap<String, Integer> httpMethodCallMap;
    private Map<String, ChannelMessageCounters> channelMessageCountersMap;

    public Builder timestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder uptimeStart(long uptimeStart) {
      this.uptimeStart = uptimeStart;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder hostname(String hostname) {
      this.hostname = hostname;
      return this;
    }

    public Builder user(String user) {
      this.user = user;
      return this;
    }

    public Builder httpMethodCallMap(ConcurrentMap<String, Integer> httpMethodCallMap) {
      this.httpMethodCallMap = httpMethodCallMap;
      return this;
    }

    public Builder channelMessageCountersMap(Map<String, ChannelMessageCounters> channelMessageCountersMap) {
      this.channelMessageCountersMap = channelMessageCountersMap;
      return this;
    }

    public StatusReportRequest build() {
      return new StatusReportRequest(timestamp, uptimeStart, name, hostname, user, httpMethodCallMap, channelMessageCountersMap);
    }
  }
}
