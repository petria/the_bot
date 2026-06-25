package org.freakz.engine.commands;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
import org.freakz.engine.data.service.UsersService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccessServiceTest {

  @Test
  void ircNickAloneDoesNotResolveRealUser() {
    AccessService accessService = new AccessService(new FixedUsersService(List.of(
        user("petria", "_Pete_", List.of()),
        unknownUser()
    )));

    User resolved = accessService.getUser(ircRequest("_Pete_", "~someone@host.invalid"));

    assertThat(resolved.getUsername()).isEqualTo("johndoe");
  }

  @Test
  void claimedIrcUserHostIdentityResolvesRealUser() {
    AccessService accessService = new AccessService(new FixedUsersService(List.of(
        user("petria", "_Pete_", List.of(UserChatIdentity.builder()
            .connectionType("IRC_CONNECTION")
            .network("IRCNet")
            .userId("~petria@host.invalid")
            .username("_Pete_")
            .source("IRC_TOKEN_CLAIM")
            .build())),
        unknownUser()
    )));

    User resolved = accessService.getUser(ircRequest("OtherNick", "~petria@host.invalid"));

    assertThat(resolved.getUsername()).isEqualTo("petria");
  }

  @Test
  void claimedIrcIdentityDoesNotResolveWhenHostDiffers() {
    AccessService accessService = new AccessService(new FixedUsersService(List.of(
        user("petria", "_Pete_", List.of(UserChatIdentity.builder()
            .connectionType("IRC_CONNECTION")
            .network("IRCNet")
            .userId("~petria@host.invalid")
            .username("_Pete_")
            .source("IRC_TOKEN_CLAIM")
            .build())),
        unknownUser()
    )));

    User resolved = accessService.getUser(ircRequest("_Pete_", "~petria@other.invalid"));

    assertThat(resolved.getUsername()).isEqualTo("johndoe");
  }

  @Test
  void cliClientResolvesByAuthenticatedUsername() {
    AccessService accessService = new AccessService(new FixedUsersService(List.of(
        user("test", "SomeoneElse", List.of(UserChatIdentity.builder()
            .connectionType("IRC_CONNECTION")
            .network("IRCNet")
            .userId("~test@host.invalid")
            .username("SomeoneElse")
            .source("IRC_TOKEN_CLAIM")
            .build())),
        unknownUser()
    )));

    User resolved = accessService.getUser(cliRequest("test", "WEB-CLI:7"));

    assertThat(resolved.getUsername()).isEqualTo("test");
  }

  @Test
  void webConsoleResolvesByAuthenticatedUsername() {
    AccessService accessService = new AccessService(new FixedUsersService(List.of(
        user("test", "SomeoneElse", List.of()),
        unknownUser()
    )));

    User resolved = accessService.getUser(consoleRequest("test", "WEB-CONSOLE:7"));

    assertThat(resolved.getUsername()).isEqualTo("test");
  }

  private EngineRequest ircRequest(String nick, String userHost) {
    return EngineRequest.builder()
        .network("IRCNet")
        .chatProtocol("irc")
        .fromSender(nick)
        .fromSenderId(userHost)
        .build();
  }

  private EngineRequest cliRequest(String username, String senderId) {
    return EngineRequest.builder()
        .network("BOT_CLI_CLIENT")
        .chatProtocol("cli")
        .fromSender(username)
        .fromSenderId(senderId)
        .build();
  }

  private EngineRequest consoleRequest(String username, String senderId) {
    return EngineRequest.builder()
        .network("BOT_WEB_CONSOLE")
        .chatProtocol("web")
        .chatType("console")
        .fromSender(username)
        .fromSenderId(senderId)
        .build();
  }

  private User user(String username, String ircNick, List<UserChatIdentity> identities) {
    User user = User.builder()
        .username(username)
        .ircNick(ircNick)
        .chatIdentities(identities)
        .build();
    user.setId(1L);
    return user;
  }

  private User unknownUser() {
    User user = User.builder()
        .username("johndoe")
        .build();
    user.setId(0L);
    return user;
  }

  private record FixedUsersService(List<User> users) implements UsersService {
    @Override
    public List<? extends DataNodeBase> findAll() {
      return users;
    }

    @Override
    public User getNotKnownUser() {
      return users.stream().filter(user -> user.getId() != null && user.getId() == 0L).findFirst().orElse(users.get(0));
    }

    @Override
    public User addChatIdentity(long userId, UserChatIdentity identity, boolean moveIfOwned) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void reloadUsers() {
    }
  }
}
