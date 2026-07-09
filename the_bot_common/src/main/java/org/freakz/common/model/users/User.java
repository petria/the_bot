package org.freakz.common.model.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.freakz.common.model.dto.DataNodeBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends DataNodeBase {

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
  @JsonProperty("whatsappId")
  private String whatsappId;
  @JsonProperty("homeChannel")
  private UserHomeChannel homeChannel;
  @JsonProperty("chatIdentities")
  private List<UserChatIdentity> chatIdentities = new ArrayList<>();
  @JsonProperty("permissions")
  private List<String> permissions = new ArrayList<>();

  public User() {
  }

  public User(String username, String password, String name, String email, String ircNick, String telegramId, String discordId, String whatsappId) {
    this.username = username;
    this.password = password;
    this.name = name;
    this.email = email;
    this.ircNick = ircNick;
    this.telegramId = telegramId;
    this.discordId = discordId;
    this.whatsappId = whatsappId;
  }

  public static Builder builder() {
    return new Builder();
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

  public String getWhatsappId() {
    return whatsappId;
  }

  public void setWhatsappId(String whatsappId) {
    this.whatsappId = whatsappId;
  }

  public UserHomeChannel getHomeChannel() {
    return homeChannel;
  }

  public void setHomeChannel(UserHomeChannel homeChannel) {
    this.homeChannel = homeChannel;
  }

  public List<UserChatIdentity> getChatIdentities() {
    return chatIdentities;
  }

  public void setChatIdentities(List<UserChatIdentity> chatIdentities) {
    this.chatIdentities = chatIdentities == null ? new ArrayList<>() : new ArrayList<>(chatIdentities);
  }

  public List<String> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<String> permissions) {
    this.permissions = permissions == null ? new ArrayList<>() : new ArrayList<>(permissions);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(username, user.username) && Objects.equals(password, user.password) && Objects.equals(name, user.name) && Objects.equals(email, user.email) && Objects.equals(ircNick, user.ircNick) && Objects.equals(telegramId, user.telegramId) && Objects.equals(discordId, user.discordId) && Objects.equals(whatsappId, user.whatsappId) && Objects.equals(homeChannel, user.homeChannel) && Objects.equals(chatIdentities, user.chatIdentities) && Objects.equals(permissions, user.permissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, password, name, email, ircNick, telegramId, discordId, whatsappId, homeChannel, chatIdentities, permissions);
  }

  @Override
  public String toString() {
    return "User{" +
        "username='" + username + '\'' +
        ", password='" + password + '\'' +
        ", name='" + name + '\'' +
        ", email='" + email + '\'' +
        ", ircNick='" + ircNick + '\'' +
        ", telegramId='" + telegramId + '\'' +
        ", discordId='" + discordId + '\'' +
        ", whatsappId='" + whatsappId + '\'' +
        ", homeChannel=" + homeChannel +
        ", chatIdentities=" + chatIdentities +
        ", permissions=" + permissions +
        '}';
  }

  public static class Builder {
    private String username;
    private String password;
    private String name;
    private String email;
    private String ircNick;
    private String telegramId;
    private String discordId;
    private String whatsappId;
    private UserHomeChannel homeChannel;
    private List<UserChatIdentity> chatIdentities = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();

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

    public Builder whatsappId(String whatsappId) {
      this.whatsappId = whatsappId;
      return this;
    }

    public Builder homeChannel(UserHomeChannel homeChannel) {
      this.homeChannel = homeChannel;
      return this;
    }

    public Builder chatIdentities(List<UserChatIdentity> chatIdentities) {
      this.chatIdentities = chatIdentities == null ? new ArrayList<>() : new ArrayList<>(chatIdentities);
      return this;
    }

    public Builder permissions(List<String> permissions) {
      this.permissions = permissions == null ? new ArrayList<>() : new ArrayList<>(permissions);
      return this;
    }

    public User build() {
      User user = new User(username, password, name, email, ircNick, telegramId, discordId, whatsappId);
      user.setHomeChannel(homeChannel);
      user.setChatIdentities(chatIdentities);
      user.setPermissions(permissions);
      return user;
    }
  }
}
