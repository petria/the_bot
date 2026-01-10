package org.freakz.common.model.slack;

import java.util.Objects;

public class UrlVerificationRequest extends UrlVerificationResponse {

    private String token;
    private String type;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UrlVerificationRequest that = (UrlVerificationRequest) o;
        return Objects.equals(token, that.token) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), token, type);
    }

    @Override
    public String toString() {
        return "UrlVerificationRequest{" +
                "token='" + token + '\'' +
                ", type='" + type + '\'' +
                "} " + super.toString();
    }
}
