package org.freakz.common.model.foreca;

import java.util.Objects;

public class ForecaData {

    private CountryCityLink cityLink;
    private ForecaWeatherData weatherData;
    private ForecaSunUpDown sunUpDown;

    public ForecaData(CountryCityLink cityLink, ForecaWeatherData weatherData, ForecaSunUpDown sunUpDown) {
        this.cityLink = cityLink;
        this.weatherData = weatherData;
        this.sunUpDown = sunUpDown;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CountryCityLink getCityLink() {
        return cityLink;
    }

    public void setCityLink(CountryCityLink cityLink) {
        this.cityLink = cityLink;
    }

    public ForecaWeatherData getWeatherData() {
        return weatherData;
    }

    public void setWeatherData(ForecaWeatherData weatherData) {
        this.weatherData = weatherData;
    }

    public ForecaSunUpDown getSunUpDown() {
        return sunUpDown;
    }

    public void setSunUpDown(ForecaSunUpDown sunUpDown) {
        this.sunUpDown = sunUpDown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForecaData that = (ForecaData) o;
        return Objects.equals(cityLink, that.cityLink) && Objects.equals(weatherData, that.weatherData) && Objects.equals(sunUpDown, that.sunUpDown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cityLink, weatherData, sunUpDown);
    }

    @Override
    public String toString() {
        return "ForecaData{" +
                "cityLink=" + cityLink +
                ", weatherData=" + weatherData +
                ", sunUpDown=" + sunUpDown +
                '}';
    }

    public static class Builder {
        private CountryCityLink cityLink;
        private ForecaWeatherData weatherData;
        private ForecaSunUpDown sunUpDown;

        public Builder cityLink(CountryCityLink cityLink) {
            this.cityLink = cityLink;
            return this;
        }

        public Builder weatherData(ForecaWeatherData weatherData) {
            this.weatherData = weatherData;
            return this;
        }

        public Builder sunUpDown(ForecaSunUpDown sunUpDown) {
            this.sunUpDown = sunUpDown;
            return this;
        }

        public ForecaData build() {
            return new ForecaData(cityLink, weatherData, sunUpDown);
        }
    }
}
