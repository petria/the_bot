package org.freakz.common.model.botconfig;

import java.util.Objects;

public class BotConfig {

    private String botName;
    private String apiKey;
    private String openAiApiKey;

    public BotConfig() {
    }

    public BotConfig(String botName, String apiKey, String openAiApiKey) {
        this.botName = botName;
        this.apiKey = apiKey;
        this.openAiApiKey = openAiApiKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    public void setOpenAiApiKey(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BotConfig botConfig = (BotConfig) o;
        return Objects.equals(botName, botConfig.botName) && Objects.equals(apiKey, botConfig.apiKey) && Objects.equals(openAiApiKey, botConfig.openAiApiKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(botName, apiKey, openAiApiKey);
    }

    @Override
    public String toString() {
        return "BotConfig{" +
                "botName='" + botName + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", openAiApiKey='" + openAiApiKey + '\'' +
                '}';
    }

    public static class Builder {
        private String botName;
        private String apiKey;
        private String openAiApiKey;

        public Builder botName(String botName) {
            this.botName = botName;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder openAiApiKey(String openAiApiKey) {
            this.openAiApiKey = openAiApiKey;
            return this;
        }

        public BotConfig build() {
            return new BotConfig(botName, apiKey, openAiApiKey);
        }
    }
}
