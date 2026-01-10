package org.freakz.common.model.botconfig;

import java.util.List;
import java.util.Objects;

public class DiscordConfig {

    private String token;
    private List<Channel> channelList;
    private boolean connectStartup;
    private Long theBotUserId;

    public DiscordConfig() {
    }

    public DiscordConfig(String token, List<Channel> channelList, boolean connectStartup, Long theBotUserId) {
        this.token = token;
        this.channelList = channelList;
        this.connectStartup = connectStartup;
        this.theBotUserId = theBotUserId;
    }

    public static Builder builder() {
        return new Builder();
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

    public Long getTheBotUserId() {
        return theBotUserId;
    }

    public void setTheBotUserId(Long theBotUserId) {
        this.theBotUserId = theBotUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscordConfig that = (DiscordConfig) o;
        return connectStartup == that.connectStartup && Objects.equals(token, that.token) && Objects.equals(channelList, that.channelList) && Objects.equals(theBotUserId, that.theBotUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, channelList, connectStartup, theBotUserId);
    }

    @Override
    public String toString() {
        return "DiscordConfig{" +
                "token='" + token + '\'' +
                ", channelList=" + channelList +
                ", connectStartup=" + connectStartup +
                ", theBotUserId=" + theBotUserId +
                '}';
    }

    public static class Builder {
        private String token;
        private List<Channel> channelList;
        private boolean connectStartup;
        private Long theBotUserId;

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

        public Builder theBotUserId(Long theBotUserId) {
            this.theBotUserId = theBotUserId;
            return this;
        }

        public DiscordConfig build() {
            return new DiscordConfig(token, channelList, connectStartup, theBotUserId);
        }
    }
}
