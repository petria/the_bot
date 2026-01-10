package org.freakz.common.model.connectionmanager;

import java.util.Objects;

public class SendIrcRawMessageByTargetAliasRequest {

    private String message;
    private String targetAlias;

    public SendIrcRawMessageByTargetAliasRequest() {
    }

    public SendIrcRawMessageByTargetAliasRequest(String message, String targetAlias) {
        this.message = message;
        this.targetAlias = targetAlias;
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

    public String getTargetAlias() {
        return targetAlias;
    }

    public void setTargetAlias(String targetAlias) {
        this.targetAlias = targetAlias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SendIrcRawMessageByTargetAliasRequest that = (SendIrcRawMessageByTargetAliasRequest) o;
        return Objects.equals(message, that.message) && Objects.equals(targetAlias, that.targetAlias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, targetAlias);
    }

    @Override
    public String toString() {
        return "SendIrcRawMessageByTargetAliasRequest{" +
                "message='" + message + '\'' +
                ", targetAlias='" + targetAlias + '\'' +
                '}';
    }

    public static class Builder {
        private String message;
        private String targetAlias;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder targetAlias(String targetAlias) {
            this.targetAlias = targetAlias;
            return this;
        }

        public SendIrcRawMessageByTargetAliasRequest build() {
            return new SendIrcRawMessageByTargetAliasRequest(message, targetAlias);
        }
    }
}
