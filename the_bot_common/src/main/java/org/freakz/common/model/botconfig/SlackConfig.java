package org.freakz.common.model.botconfig;

import java.util.List;
import java.util.Objects;

public class SlackConfig {

    private String slackToken;
    private String botSlackUserId;
    private List<Channel> channelList;
    private boolean connectStartup;

    public SlackConfig() {
    }

    public SlackConfig(String slackToken, String botSlackUserId, List<Channel> channelList, boolean connectStartup) {
        this.slackToken = slackToken;
        this.botSlackUserId = botSlackUserId;
        this.channelList = channelList;
        this.connectStartup = connectStartup;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSlackToken() {
        return slackToken;
    }

    public void setSlackToken(String slackToken) {
        this.slackToken = slackToken;
    }

    public String getBotSlackUserId() {
        return botSlackUserId;
    }

    public void setBotSlackUserId(String botSlackUserId) {
        this.botSlackUserId = botSlackUserId;
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
        SlackConfig that = (SlackConfig) o;
        return connectStartup == that.connectStartup && Objects.equals(slackToken, that.slackToken) && Objects.equals(botSlackUserId, that.botSlackUserId) && Objects.equals(channelList, that.channelList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slackToken, botSlackUserId, channelList, connectStartup);
    }

    @Override
    public String toString() {
        return "SlackConfig{" +
                "slackToken='" + slackToken + '\'' +
                ", botSlackUserId='" + botSlackUserId + '\'' +
                ", channelList=" + channelList +
                ", connectStartup=" + connectStartup +
                '}';
    }

    public static class Builder {
        private String slackToken;
        private String botSlackUserId;
        private List<Channel> channelList;
        private boolean connectStartup;

        public Builder slackToken(String slackToken) {
            this.slackToken = slackToken;
            return this;
        }

        public Builder botSlackUserId(String botSlackUserId) {
            this.botSlackUserId = botSlackUserId;
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

        public SlackConfig build() {
            return new SlackConfig(slackToken, botSlackUserId, channelList, connectStartup);
        }
    }
}
