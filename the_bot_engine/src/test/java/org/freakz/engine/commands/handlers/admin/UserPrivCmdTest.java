package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.common.users.BotPermission;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.data.service.UsersService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPrivCmdTest {

  @Test
  void grantingSendForConnectionTypeAlsoGrantsView() throws Exception {
    User user = user("petria", List.of(BotPermission.WEB_USER));
    UserPrivCmd cmd = command(new MutableUsersService(user));

    String reply = cmd.executeCommand(request(), parse(cmd, "grant petria send irc"));

    assertThat(reply).contains("Granted channels.view.irc, channels.send.irc");
    assertThat(user.getPermissions()).contains("channels.view.irc", "channels.send.irc");
  }

  @Test
  void revokingViewForConnectionTypeAlsoRevokesSend() throws Exception {
    User user = user("petria", List.of("channels.view.irc", "channels.send.irc"));
    UserPrivCmd cmd = command(new MutableUsersService(user));

    String reply = cmd.executeCommand(request(), parse(cmd, "revoke petria view irc"));

    assertThat(reply).contains("Revoked channels.view.irc, channels.send.irc");
    assertThat(user.getPermissions()).doesNotContain("channels.view.irc", "channels.send.irc");
  }

  @Test
  void revokingSendForConnectionTypeKeepsView() throws Exception {
    User user = user("petria", List.of("channels.view.irc", "channels.send.irc"));
    UserPrivCmd cmd = command(new MutableUsersService(user));

    String reply = cmd.executeCommand(request(), parse(cmd, "revoke petria send irc"));

    assertThat(reply).contains("Revoked channels.send.irc");
    assertThat(user.getPermissions())
        .contains("channels.view.irc")
        .doesNotContain("channels.send.irc");
  }

  @Test
  void migrateConvertsLegacyAllPermissions() throws Exception {
    User user = user("petria", List.of("live-channels.view.all", "live-channels.send.all"));
    MutableUsersService usersService = new MutableUsersService(user);
    UserPrivCmd cmd = command(usersService);

    String reply = cmd.executeCommand(request(), parse(cmd, "migrate petria"));

    assertThat(reply).contains("petria: added");
    assertThat(user.getPermissions())
        .contains("channels.view.all", "channels.send.all")
        .doesNotContain("live-channels.view.all", "live-channels.send.all");
    assertThat(usersService.reloadCount).isEqualTo(1);
  }

  private UserPrivCmd command(UsersService usersService) {
    BotEngine botEngine = mock(BotEngine.class);
    when(botEngine.getUsersService()).thenReturn(usersService);
    UserPrivCmd cmd = new UserPrivCmd();
    cmd.setBotEngine(botEngine);
    return cmd;
  }

  private JSAPResult parse(UserPrivCmd cmd, String args) throws Exception {
    JSAP jsap = cmd.getJsap();
    cmd.abstractInitCommandOptions();
    return jsap.parse(args);
  }

  private EngineRequest request() {
    return EngineRequest.builder().build();
  }

  private User user(String username, List<String> permissions) {
    User user = User.builder()
        .username(username)
        .permissions(permissions)
        .build();
    user.setId(1L);
    return user;
  }

  private static class MutableUsersService implements UsersService {
    private final User user;
    private int reloadCount;

    MutableUsersService(User user) {
      this.user = user;
    }

    @Override
    public List<User> findAll() {
      return List.of(user);
    }

    @Override
    public User getNotKnownUser() {
      return user;
    }

    @Override
    public User addChatIdentity(long userId, org.freakz.common.model.users.UserChatIdentity identity, boolean moveIfOwned) {
      throw new UnsupportedOperationException();
    }

    @Override
    public User updateByUsername(String username, UnaryOperator<User> updater) {
      if (!user.getUsername().equalsIgnoreCase(username)) {
        throw new IllegalArgumentException("No bot user found for username: " + username);
      }
      User updated = updater.apply(user);
      user.setPermissions(new ArrayList<>(updated.getPermissions()));
      return user;
    }

    @Override
    public void reloadUsers() {
      reloadCount++;
    }
  }
}
