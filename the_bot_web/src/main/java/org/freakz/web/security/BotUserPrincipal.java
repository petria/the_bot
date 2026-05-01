package org.freakz.web.security;

import org.freakz.common.model.users.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class BotUserPrincipal implements UserDetails {

  private final Long id;
  private final String username;
  private final String password;
  private final String name;
  private final String email;
  private final String ircNick;
  private final String telegramId;
  private final String discordId;
  private final boolean admin;
  private final boolean canDoIrcOp;
  private final List<GrantedAuthority> authorities;

  public BotUserPrincipal(
      Long id,
      String username,
      String password,
      String name,
      String email,
      String ircNick,
      String telegramId,
      String discordId,
      boolean admin,
      boolean canDoIrcOp,
      List<GrantedAuthority> authorities) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.name = name;
    this.email = email;
    this.ircNick = ircNick;
    this.telegramId = telegramId;
    this.discordId = discordId;
    this.admin = admin;
    this.canDoIrcOp = canDoIrcOp;
    this.authorities = List.copyOf(authorities);
  }

  public static BotUserPrincipal from(User user, List<GrantedAuthority> authorities) {
    return new BotUserPrincipal(
        user.getId(),
        user.getUsername(),
        user.getPassword(),
        user.getName(),
        user.getEmail(),
        user.getIrcNick(),
        user.getTelegramId(),
        user.getDiscordId(),
        user.isAdmin(),
        user.isCanDoIrcOp(),
        authorities);
  }

  public Long getId() {
    return id;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getIrcNick() {
    return ircNick;
  }

  public String getTelegramId() {
    return telegramId;
  }

  public String getDiscordId() {
    return discordId;
  }

  public boolean isAdmin() {
    return admin;
  }

  public boolean isCanDoIrcOp() {
    return canDoIrcOp;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }
}
