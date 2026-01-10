package org.freakz.common.model.botconfig;

import java.util.List;
import java.util.Objects;

public class TelegramConfig {

    private String telegramName;
    private String token;
    private List<Channel> channelList;
    private boolean connectStartup;

    public TelegramConfig() {
    }

    public TelegramConfig(String telegramName, String token, List<Channel> channelList, boolean connectStartup) {
        this.telegramName = telegramName;
        this.token = token;
        this.channelList = channelList;
        this.connectStartup = connectStartup;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTelegramName() {
        return telegramName;
    }

    public void setTelegramName(String telegramName) {
        this.telegramName = telegramName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<Channel> getChannelList() {
        return channelList;
    }

    public void setChannelList(List<Channel> channelList) {
        this.channelList = channelList;
    }

    public boolean isConnectStartup() {
        return connectStartup;
    }

    public void setConnectStartup(boolean connectStartup) {
        this.connectStartup = connectStartup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelegramConfig that = (TelegramConfig) o;
        return connectStartup == that.connectStartup && Objects.equals(telegramName, that.telegramName) && Objects.equals(token, that.token) && Objects.equals(channelList, that.channelList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(telegramName, token, channelList, connectStartup);
    }

    @Override
    public String toString() {
        return "TelegramConfig{" +
                "telegramName='" + telegramName + '\'' +
                ", token='" + token + '\'' +
                ", channelList=" + channelList +
                ", connectStartup=" + connectStartup +
                '}';
    }

    public static class Builder {
        private String telegramName;
        private String token;
        private List<Channel> channelList;
        private boolean connectStartup;

        public Builder telegramName(String telegramName) {
            this.telegramName = telegramName;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder channelList(List<Channel> channelList) {
            this.channelList = channelList;
            return this;
        }

        public Builder connectStartup(boolean connectStartup) {
            this.connectStartup = connectStartup;
            return this;
        }

        public TelegramConfig build() {
            return new TelegramConfig(telegramName, token, channelList, connectStartup);
        }
    }
}
