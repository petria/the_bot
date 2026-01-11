package org.freakz.engine.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Created by Petri Airio on 23.6.2015. -
 */
public class KelikameratWeatherData implements Serializable {

  private LocalDateTime time;

  private KelikameratUrl url;

  private String place;
  private String placeFromUrl;

  private Float air;
  private Float road;
  private Float ground;

  private Float humidity;
  private Float dewPoint;

  public KelikameratWeatherData() {
  }

  public KelikameratWeatherData(LocalDateTime time, KelikameratUrl url, String place, String placeFromUrl, Float air, Float road, Float ground, Float humidity, Float dewPoint) {
    this.time = time;
    this.url = url;
    this.place = place;
    this.placeFromUrl = placeFromUrl;
    this.air = air;
    this.road = road;
    this.ground = ground;
    this.humidity = humidity;
    this.dewPoint = dewPoint;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public void setTime(LocalDateTime time) {
    this.time = time;
  }

  public KelikameratUrl getUrl() {
    return url;
  }

  public void setUrl(KelikameratUrl url) {
    this.url = url;
  }

  public String getPlace() {
    return place;
  }

  public void setPlace(String place) {
    this.place = place;
  }

  public String getPlaceFromUrl() {
    return placeFromUrl;
  }

  public void setPlaceFromUrl(String placeFromUrl) {
    this.placeFromUrl = placeFromUrl;
  }

  public Float getAir() {
    return air;
  }

  public void setAir(Float air) {
    this.air = air;
  }

  public Float getRoad() {
    return road;
  }

  public void setRoad(Float road) {
    this.road = road;
  }

  public Float getGround() {
    return ground;
  }

  public void setGround(Float ground) {
    this.ground = ground;
  }

  public Float getHumidity() {
    return humidity;
  }

  public void setHumidity(Float humidity) {
    this.humidity = humidity;
  }

  public Float getDewPoint() {
    return dewPoint;
  }

  public void setDewPoint(Float dewPoint) {
    this.dewPoint = dewPoint;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    KelikameratWeatherData that = (KelikameratWeatherData) o;

    if (time != null ? !time.equals(that.time) : that.time != null) return false;
    if (url != null ? !url.equals(that.url) : that.url != null) return false;
    if (place != null ? !place.equals(that.place) : that.place != null) return false;
    if (placeFromUrl != null ? !placeFromUrl.equals(that.placeFromUrl) : that.placeFromUrl != null) return false;
    if (air != null ? !air.equals(that.air) : that.air != null) return false;
    if (road != null ? !road.equals(that.road) : that.road != null) return false;
    if (ground != null ? !ground.equals(that.ground) : that.ground != null) return false;
    if (humidity != null ? !humidity.equals(that.humidity) : that.humidity != null) return false;
    return dewPoint != null ? dewPoint.equals(that.dewPoint) : that.dewPoint == null;
  }

  @Override
  public int hashCode() {
    int result = time != null ? time.hashCode() : 0;
    result = 31 * result + (url != null ? url.hashCode() : 0);
    result = 31 * result + (place != null ? place.hashCode() : 0);
    result = 31 * result + (placeFromUrl != null ? placeFromUrl.hashCode() : 0);
    result = 31 * result + (air != null ? air.hashCode() : 0);
    result = 31 * result + (road != null ? road.hashCode() : 0);
    result = 31 * result + (ground != null ? ground.hashCode() : 0);
    result = 31 * result + (humidity != null ? humidity.hashCode() : 0);
    result = 31 * result + (dewPoint != null ? dewPoint.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "KelikameratWeatherData{" +
        "time=" + time +
        ", url=" + url +
        ", place='" + place + '\'' +
        ", placeFromUrl='" + placeFromUrl + '\'' +
        ", air=" + air +
        ", road=" + road +
        ", ground=" + ground +
        ", humidity=" + humidity +
        ", dewPoint=" + dewPoint +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private LocalDateTime time;
    private KelikameratUrl url;
    private String place;
    private String placeFromUrl;
    private Float air;
    private Float road;
    private Float ground;
    private Float humidity;
    private Float dewPoint;

    Builder() {
    }

    public Builder time(LocalDateTime time) {
      this.time = time;
      return this;
    }

    public Builder url(KelikameratUrl url) {
      this.url = url;
      return this;
    }

    public Builder place(String place) {
      this.place = place;
      return this;
    }

    public Builder placeFromUrl(String placeFromUrl) {
      this.placeFromUrl = placeFromUrl;
      return this;
    }

    public Builder air(Float air) {
      this.air = air;
      return this;
    }

    public Builder road(Float road) {
      this.road = road;
      return this;
    }

    public Builder ground(Float ground) {
      this.ground = ground;
      return this;
    }

    public Builder humidity(Float humidity) {
      this.humidity = humidity;
      return this;
    }

    public Builder dewPoint(Float dewPoint) {
      this.dewPoint = dewPoint;
      return this;
    }

    public KelikameratWeatherData build() {
      return new KelikameratWeatherData(time, url, place, placeFromUrl, air, road, ground, humidity, dewPoint);
    }
  }
}
