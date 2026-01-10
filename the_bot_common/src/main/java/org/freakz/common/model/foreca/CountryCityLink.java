package org.freakz.common.model.foreca;

import java.util.Objects;

public class CountryCityLink {

    public String region;
    public String country;
    public String city;
    public String city2;
    public String cityUrl;

    public CountryCityLink() {
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity2() {
        return city2;
    }

    public void setCity2(String city2) {
        this.city2 = city2;
    }

    public String getCityUrl() {
        return cityUrl;
    }

    public void setCityUrl(String cityUrl) {
        this.cityUrl = cityUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CountryCityLink that = (CountryCityLink) o;
        return Objects.equals(region, that.region) && Objects.equals(country, that.country) && Objects.equals(city, that.city) && Objects.equals(city2, that.city2) && Objects.equals(cityUrl, that.cityUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, country, city, city2, cityUrl);
    }

    @Override
    public String toString() {
        return "CountryCityLink{" +
                "region='" + region + '\'' +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", city2='" + city2 + '\'' +
                ", cityUrl='" + cityUrl + '\'' +
                '}';
    }
}
