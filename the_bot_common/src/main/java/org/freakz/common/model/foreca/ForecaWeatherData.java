package org.freakz.common.model.foreca;

import java.util.Objects;

public class ForecaWeatherData {

    private Integer key;
    private String date;
    private String time;
    private Double temp;
    private Double feelsLike;
    private Double relativeHumidity;
    private Double visibility;
    private String visibilityUnit;
    private Double pressure;

    public ForecaWeatherData(Integer key, String date, String time, Double temp, Double feelsLike, Double relativeHumidity, Double visibility, String visibilityUnit, Double pressure) {
        this.key = key;
        this.date = date;
        this.time = time;
        this.temp = temp;
        this.feelsLike = feelsLike;
        this.relativeHumidity = relativeHumidity;
        this.visibility = visibility;
        this.visibilityUnit = visibilityUnit;
        this.pressure = pressure;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Double getTemp() {
        return temp;
    }

    public void setTemp(Double temp) {
        this.temp = temp;
    }

    public Double getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(Double feelsLike) {
        this.feelsLike = feelsLike;
    }

    public Double getRelativeHumidity() {
        return relativeHumidity;
    }

    public void setRelativeHumidity(Double relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
    }

    public Double getVisibility() {
        return visibility;
    }

    public void setVisibility(Double visibility) {
        this.visibility = visibility;
    }

    public String getVisibilityUnit() {
        return visibilityUnit;
    }

    public void setVisibilityUnit(String visibilityUnit) {
        this.visibilityUnit = visibilityUnit;
    }

    public Double getPressure() {
        return pressure;
    }

    public void setPressure(Double pressure) {
        this.pressure = pressure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForecaWeatherData that = (ForecaWeatherData) o;
        return Objects.equals(key, that.key) && Objects.equals(date, that.date) && Objects.equals(time, that.time) && Objects.equals(temp, that.temp) && Objects.equals(feelsLike, that.feelsLike) && Objects.equals(relativeHumidity, that.relativeHumidity) && Objects.equals(visibility, that.visibility) && Objects.equals(visibilityUnit, that.visibilityUnit) && Objects.equals(pressure, that.pressure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, date, time, temp, feelsLike, relativeHumidity, visibility, visibilityUnit, pressure);
    }

    @Override
    public String toString() {
        return "ForecaWeatherData{" +
                "key=" + key +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", temp=" + temp +
                ", feelsLike=" + feelsLike +
                ", relativeHumidity=" + relativeHumidity +
                ", visibility=" + visibility +
                ", visibilityUnit='" + visibilityUnit + '\'' +
                ", pressure=" + pressure +
                '}';
    }

    public static class Builder {
        private Integer key;
        private String date;
        private String time;
        private Double temp;
        private Double feelsLike;
        private Double relativeHumidity;
        private Double visibility;
        private String visibilityUnit;
        private Double pressure;

        public Builder key(Integer key) {
            this.key = key;
            return this;
        }

        public Builder date(String date) {
            this.date = date;
            return this;
        }

        public Builder time(String time) {
            this.time = time;
            return this;
        }

        public Builder temp(Double temp) {
            this.temp = temp;
            return this;
        }

        public Builder feelsLike(Double feelsLike) {
            this.feelsLike = feelsLike;
            return this;
        }

        public Builder relativeHumidity(Double relativeHumidity) {
            this.relativeHumidity = relativeHumidity;
            return this;
        }

        public Builder visibility(Double visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder visibilityUnit(String visibilityUnit) {
            this.visibilityUnit = visibilityUnit;
            return this;
        }

        public Builder pressure(Double pressure) {
            this.pressure = pressure;
            return this;
        }

        public ForecaWeatherData build() {
            return new ForecaWeatherData(key, date, time, temp, feelsLike, relativeHumidity, visibility, visibilityUnit, pressure);
        }
    }
}
