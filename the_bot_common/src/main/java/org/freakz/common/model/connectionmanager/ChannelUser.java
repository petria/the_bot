package org.freakz.common.model.connectionmanager;

import java.util.Objects;

public class ChannelUser {

    private String account;
    private String awayMessage;
    private String host;
    private String nick;
    private String operatorInformation;
    private String realName;
    private String server;
    private String userString;
    private boolean isAway;

    public ChannelUser() {
    }

    public ChannelUser(String account, String awayMessage, String host, String nick, String operatorInformation, String realName, String server, String userString, boolean isAway) {
        this.account = account;
        this.awayMessage = awayMessage;
        this.host = host;
        this.nick = nick;
        this.operatorInformation = operatorInformation;
        this.realName = realName;
        this.server = server;
        this.userString = userString;
        this.isAway = isAway;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAwayMessage() {
        return awayMessage;
    }

    public void setAwayMessage(String awayMessage) {
        this.awayMessage = awayMessage;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getOperatorInformation() {
        return operatorInformation;
    }

    public void setOperatorInformation(String operatorInformation) {
        this.operatorInformation = operatorInformation;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUserString() {
        return userString;
    }

    public void setUserString(String userString) {
        this.userString = userString;
    }

    public boolean isAway() {
        return isAway;
    }

    public void setAway(boolean away) {
        isAway = away;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelUser that = (ChannelUser) o;
        return isAway == that.isAway && Objects.equals(account, that.account) && Objects.equals(awayMessage, that.awayMessage) && Objects.equals(host, that.host) && Objects.equals(nick, that.nick) && Objects.equals(operatorInformation, that.operatorInformation) && Objects.equals(realName, that.realName) && Objects.equals(server, that.server) && Objects.equals(userString, that.userString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(account, awayMessage, host, nick, operatorInformation, realName, server, userString, isAway);
    }

    @Override
    public String toString() {
        return "ChannelUser{" +
                "account='" + account + '\'' +
                ", awayMessage='" + awayMessage + '\'' +
                ", host='" + host + '\'' +
                ", nick='" + nick + '\'' +
                ", operatorInformation='" + operatorInformation + '\'' +
                ", realName='" + realName + '\'' +
                ", server='" + server + '\'' +
                ", userString='" + userString + '\'' +
                ", isAway=" + isAway +
                '}';
    }

    public static class Builder {
        private String account;
        private String awayMessage;
        private String host;
        private String nick;
        private String operatorInformation;
        private String realName;
        private String server;
        private String userString;
        private boolean isAway;

        public Builder account(String account) {
            this.account = account;
            return this;
        }

        public Builder awayMessage(String awayMessage) {
            this.awayMessage = awayMessage;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder nick(String nick) {
            this.nick = nick;
            return this;
        }

        public Builder operatorInformation(String operatorInformation) {
            this.operatorInformation = operatorInformation;
            return this;
        }

        public Builder realName(String realName) {
            this.realName = realName;
            return this;
        }

        public Builder server(String server) {
            this.server = server;
            return this;
        }

        public Builder userString(String userString) {
            this.userString = userString;
            return this;
        }

        public Builder isAway(boolean isAway) {
            this.isAway = isAway;
            return this;
        }

        public ChannelUser build() {
            return new ChannelUser(account, awayMessage, host, nick, operatorInformation, realName, server, userString, isAway);
        }
    }
}
