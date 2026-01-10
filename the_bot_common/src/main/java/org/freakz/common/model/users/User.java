package org.freakz.common.model.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.freakz.common.model.dto.DataNodeBase;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends DataNodeBase {

    @JsonProperty("isAdmin")
    private boolean isAdmin;
    @JsonProperty("canDoIrcOp")
    private boolean canDoIrcOp;
    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;
    @JsonProperty("name")
    private String name;
    @JsonProperty("email")
    private String email;
    @JsonProperty("ircNick")
    private String ircNick;
    @JsonProperty("telegramId")
    private String telegramId;
    @JsonProperty("discordId")
    private String discordId;
    @JsonProperty("slackId")
    private String slackId;

    public User() {
    }

    public User(boolean isAdmin, boolean canDoIrcOp, String username, String password, String name, String email, String ircNick, String telegramId, String discordId, String slackId) {
        this.isAdmin = isAdmin;
        this.canDoIrcOp = canDoIrcOp;
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.ircNick = ircNick;
        this.telegramId = telegramId;
        this.discordId = discordId;
        this.slackId = slackId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isCanDoIrcOp() {
        return canDoIrcOp;
    }

    public void setCanDoIrcOp(boolean canDoIrcOp) {
        this.canDoIrcOp = canDoIrcOp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIrcNick() {
        return ircNick;
    }

    public void setIrcNick(String ircNick) {
        this.ircNick = ircNick;
    }

    public String getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(String telegramId) {
        this.telegramId = telegramId;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getSlackId() {
        return slackId;
    }

    public void setSlackId(String slackId) {
        this.slackId = slackId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return isAdmin == user.isAdmin && canDoIrcOp == user.canDoIrcOp && Objects.equals(username, user.username) && Objects.equals(password, user.password) && Objects.equals(name, user.name) && Objects.equals(email, user.email) && Objects.equals(ircNick, user.ircNick) && Objects.equals(telegramId, user.telegramId) && Objects.equals(discordId, user.discordId) && Objects.equals(slackId, user.slackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isAdmin, canDoIrcOp, username, password, name, email, ircNick, telegramId, discordId, slackId);
    }

    @Override
    public String toString() {
        return "User{" +
                "isAdmin=" + isAdmin +
                ", canDoIrcOp=" + canDoIrcOp +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", ircNick='" + ircNick + '\'' +
                ", telegramId='" + telegramId + '\'' +
                ", discordId='" + discordId + '\'' +
                ", slackId='" + slackId + '\'' +
                '}';
    }

    public static class Builder {
        private boolean isAdmin;
        private boolean canDoIrcOp;
        private String username;
        private String password;
        private String name;
        private String email;
        private String ircNick;
        private String telegramId;
        private String discordId;
        private String slackId;

        public Builder isAdmin(boolean isAdmin) {
            this.isAdmin = isAdmin;
            return this;
        }

        public Builder canDoIrcOp(boolean canDoIrcOp) {
            this.canDoIrcOp = canDoIrcOp;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder ircNick(String ircNick) {
            this.ircNick = ircNick;
            return this;
        }

        public Builder telegramId(String telegramId) {
            this.telegramId = telegramId;
            return this;
        }

        public Builder discordId(String discordId) {
            this.discordId = discordId;
            return this;
        }

        public Builder slackId(String slackId) {
            this.slackId = slackId;
            return this;
        }

        public User build() {
            return new User(isAdmin, canDoIrcOp, username, password, name, email, ircNick, telegramId, discordId, slackId);
        }
    }
}
