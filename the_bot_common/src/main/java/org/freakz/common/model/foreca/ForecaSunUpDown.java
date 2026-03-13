package org.freakz.common.model.foreca;

import java.util.Objects;

public class ForecaSunUpDown {

  private int dayLengthTotalMinutes;
  private int dayLengthHours;
  private int dayLengthMinutes;
  private String sunUpTime;
  private String sunDownTime;

  public ForecaSunUpDown(int dayLengthTotalMinutes, int dayLengthHours, int dayLengthMinutes, String sunUpTime, String sunDownTime) {
    this.dayLengthTotalMinutes = dayLengthTotalMinutes;
    this.dayLengthHours = dayLengthHours;
    this.dayLengthMinutes = dayLengthMinutes;
    this.sunUpTime = sunUpTime;
    this.sunDownTime = sunDownTime;
  }

  public static Builder builder() {
    return new Builder();
  }

  public int getDayLengthTotalMinutes() {
    return dayLengthTotalMinutes;
  }

  public void setDayLengthTotalMinutes(int dayLengthTotalMinutes) {
    this.dayLengthTotalMinutes = dayLengthTotalMinutes;
  }

  public int getDayLengthHours() {
    return dayLengthHours;
  }

  public void setDayLengthHours(int dayLengthHours) {
    this.dayLengthHours = dayLengthHours;
  }

  public int getDayLengthMinutes() {
    return dayLengthMinutes;
  }

  public void setDayLengthMinutes(int dayLengthMinutes) {
    this.dayLengthMinutes = dayLengthMinutes;
  }

  public String getSunUpTime() {
    return sunUpTime;
  }

  public void setSunUpTime(String sunUpTime) {
    this.sunUpTime = sunUpTime;
  }

  public String getSunDownTime() {
    return sunDownTime;
  }

  public void setSunDownTime(String sunDownTime) {
    this.sunDownTime = sunDownTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ForecaSunUpDown that = (ForecaSunUpDown) o;
    return dayLengthTotalMinutes == that.dayLengthTotalMinutes && dayLengthHours == that.dayLengthHours && dayLengthMinutes == that.dayLengthMinutes && Objects.equals(sunUpTime, that.sunUpTime) && Objects.equals(sunDownTime, that.sunDownTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dayLengthTotalMinutes, dayLengthHours, dayLengthMinutes, sunUpTime, sunDownTime);
  }

  @Override
  public String toString() {
    return "ForecaSunUpDown{" +
        "dayLengthTotalMinutes=" + dayLengthTotalMinutes +
        ", dayLengthHours=" + dayLengthHours +
        ", dayLengthMinutes=" + dayLengthMinutes +
        ", sunUpTime='" + sunUpTime + '\'' +
        ", sunDownTime='" + sunDownTime + '\'' +
        '}';
  }

  public static class Builder {
    private int dayLengthTotalMinutes;
    private int dayLengthHours;
    private int dayLengthMinutes;
    private String sunUpTime;
    private String sunDownTime;

    public Builder dayLengthTotalMinutes(int dayLengthTotalMinutes) {
      this.dayLengthTotalMinutes = dayLengthTotalMinutes;
      return this;
    }

    public Builder dayLengthHours(int dayLengthHours) {
      this.dayLengthHours = dayLengthHours;
      return this;
    }

    public Builder dayLengthMinutes(int dayLengthMinutes) {
      this.dayLengthMinutes = dayLengthMinutes;
      return this;
    }

    public Builder sunUpTime(String sunUpTime) {
      this.sunUpTime = sunUpTime;
      return this;
    }

    public Builder sunDownTime(String sunDownTime) {
      this.sunDownTime = sunDownTime;
      return this;
    }

    public ForecaSunUpDown build() {
      return new ForecaSunUpDown(dayLengthTotalMinutes, dayLengthHours, dayLengthMinutes, sunUpTime, sunDownTime);
    }
  }
}
