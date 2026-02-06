package org.freakz.engine.dto;

import java.io.Serializable;

/**
 * Created by Petri Airio on 22.6.2015.
 */
public class KelikameratUrl implements Serializable {

  private String areaUrl;
  private String stationUrl;

  public KelikameratUrl() {
  }

  public KelikameratUrl(String areaUrl, String stationUrl) {
    this.areaUrl = areaUrl;
    this.stationUrl = stationUrl;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getAreaUrl() {
    return areaUrl;
  }

  public void setAreaUrl(String areaUrl) {
    this.areaUrl = areaUrl;
  }

  public String getStationUrl() {
    return stationUrl;
  }

  public void setStationUrl(String stationUrl) {
    this.stationUrl = stationUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    KelikameratUrl that = (KelikameratUrl) o;

    if (areaUrl != null ? !areaUrl.equals(that.areaUrl) : that.areaUrl != null) return false;
    return stationUrl != null ? stationUrl.equals(that.stationUrl) : that.stationUrl == null;
  }

  @Override
  public int hashCode() {
    int result = areaUrl != null ? areaUrl.hashCode() : 0;
    result = 31 * result + (stationUrl != null ? stationUrl.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "KelikameratUrl{" +
        "areaUrl='" + areaUrl + '\'' +
        ", stationUrl='" + stationUrl + '\'' +
        '}';
  }

  public static class Builder {
    private String areaUrl;
    private String stationUrl;

    Builder() {
    }

    public Builder areaUrl(String areaUrl) {
      this.areaUrl = areaUrl;
      return this;
    }

    public Builder stationUrl(String stationUrl) {
      this.stationUrl = stationUrl;
      return this;
    }

    public KelikameratUrl build() {
      return new KelikameratUrl(areaUrl, stationUrl);
    }
  }
}
