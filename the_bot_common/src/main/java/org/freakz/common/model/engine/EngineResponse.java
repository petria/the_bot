package org.freakz.common.model.engine;

import org.freakz.common.model.users.User;

import java.util.Objects;

public class EngineResponse {

    private String message;
    private User user;

    public EngineResponse() {
    }

    public EngineResponse(String message, User user) {
        this.message = message;
        this.user = user;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EngineResponse that = (EngineResponse) o;
        return Objects.equals(message, that.message) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, user);
    }

    @Override
    public String toString() {
        return "EngineResponse{" +
                "message='" + message + '\'' +
                ", user=" + user +
                '}';
    }

    public static class Builder {
        private String message;
        private User user;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public EngineResponse build() {
            return new EngineResponse(message, user);
        }
    }
}
