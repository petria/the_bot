package org.freakz.common.model.engine.status;

import java.util.Objects;

public class StatusReportResponse {

    private String message;

    public StatusReportResponse() {
    }

    public StatusReportResponse(String message) {
        this.message = message;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatusReportResponse that = (StatusReportResponse) o;
        return Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message);
    }

    @Override
    public String toString() {
        return "StatusReportResponse{" +
                "message='" + message + '\'' +
                '}';
    }

    public static class Builder {
        private String message;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public StatusReportResponse build() {
            return new StatusReportResponse(message);
        }
    }
}
