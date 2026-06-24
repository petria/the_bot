package org.freakz.engine.services.notifications;

import org.freakz.common.model.connectionmanager.SendMessageToKnownUserResponse;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.notify.UserNotifyRule;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.connections.ConnectionManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserNotifyRuleServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void mentionPresetRoutesPublicChannelMatchToUserPrivateWhatsapp() {
    Fixture fixture = fixture();
    fixture.service.create("pete", rule(UserNotifyRule.PATTERN_TYPE_PRESET_MENTION, null));

    fixture.service.processInboundMessage(request("_Pete_: hey", "friend"));

    verify(fixture.connectionManagerService).sendMessageToKnownUser(
        eq("pete"),
        contains("Notify match in IRCNet / #lowlife from friend: _Pete_: hey"),
        eq(true),
        eq("WHATSAPP_CONNECTION"),
        isNull());
  }

  @Test
  void mentionPresetMatchesLinkedChatIdentityDisplayName() {
    Fixture fixture = fixture(ownerWithoutLegacyIrcNick());
    fixture.service.create("pete", rule(UserNotifyRule.PATTERN_TYPE_PRESET_MENTION, null));

    fixture.service.processInboundMessage(request("_Pete_: hey", "friend"));

    verify(fixture.connectionManagerService).sendMessageToKnownUser(
        eq("pete"),
        contains("_Pete_: hey"),
        eq(true),
        eq("WHATSAPP_CONNECTION"),
        isNull());
  }

  @Test
  void regexPatternRoutesMatchingMessage() {
    Fixture fixture = fixture();
    fixture.service.create("pete", rule(UserNotifyRule.PATTERN_TYPE_REGEX, "\\bdeploy failed\\b"));

    fixture.service.processInboundMessage(request("Deploy failed on main", "friend"));

    verify(fixture.connectionManagerService).sendMessageToKnownUser(
        eq("pete"),
        contains("Deploy failed on main"),
        eq(true),
        eq("WHATSAPP_CONNECTION"),
        isNull());
  }

  @Test
  void privateMessagesAreIgnored() {
    Fixture fixture = fixture();
    fixture.service.create("pete", rule(UserNotifyRule.PATTERN_TYPE_PRESET_MENTION, null));
    EngineRequest request = request("_Pete_: hey", "friend");
    request.setPrivateChannel(true);

    fixture.service.processInboundMessage(request);

    verify(fixture.connectionManagerService, never()).sendMessageToKnownUser(
        eq("pete"),
        contains("Notify match"),
        eq(true),
        eq("WHATSAPP_CONNECTION"),
        isNull());
  }

  @Test
  void ownerMessagesAreIgnored() {
    Fixture fixture = fixture();
    fixture.service.create("pete", rule(UserNotifyRule.PATTERN_TYPE_PRESET_MENTION, null));

    fixture.service.processInboundMessage(request("_Pete_: note to self", "_Pete_"));

    verify(fixture.connectionManagerService, never()).sendMessageToKnownUser(
        eq("pete"),
        contains("Notify match"),
        eq(true),
        eq("WHATSAPP_CONNECTION"),
        isNull());
  }

  @Test
  void cooldownSuppressesRepeatedMatches() {
    Fixture fixture = fixture();
    fixture.service.create("pete", rule(UserNotifyRule.PATTERN_TYPE_PRESET_MENTION, null));

    fixture.service.processInboundMessage(request("_Pete_: first", "friend"));
    fixture.service.processInboundMessage(request("_Pete_: second", "friend"));

    verify(fixture.connectionManagerService).sendMessageToKnownUser(
        eq("pete"),
        contains("_Pete_: first"),
        eq(true),
        eq("WHATSAPP_CONNECTION"),
        isNull());
    verify(fixture.connectionManagerService, never()).sendMessageToKnownUser(
        eq("pete"),
        contains("_Pete_: second"),
        eq(true),
        eq("WHATSAPP_CONNECTION"),
        isNull());
  }

  private Fixture fixture() {
    User owner = User.builder()
        .username("pete")
        .name("Pete")
        .ircNick("_Pete_")
        .whatsappId("358501234567")
        .build();
    return fixture(owner);
  }

  private Fixture fixture(User owner) {
    List<DataNodeBase> users = List.of(owner);
    UsersService usersService = new StaticUsersService(users);
    ConnectionManagerService connectionManagerService = mock(ConnectionManagerService.class);
    when(connectionManagerService.sendMessageToKnownUser(
        eq("pete"),
        contains("Notify match"),
        eq(true),
        eq("WHATSAPP_CONNECTION"),
        isNull()))
        .thenReturn(new SendMessageToKnownUserResponse("OK", "whatsapp:358501234567", null, null, List.of()));
    UserNotifyRuleStore store = new UserNotifyRuleStore(
        tempDir.resolve("user-notify-rules.json"),
        JsonMapper.builder().build());
    return new Fixture(new UserNotifyRuleService(store, usersService, connectionManagerService), connectionManagerService);
  }

  private User ownerWithoutLegacyIrcNick() {
    return User.builder()
        .username("pete")
        .name("Pete")
        .whatsappId("358501234567")
        .chatIdentities(List.of(UserChatIdentity.builder()
            .connectionType("IRC_CONNECTION")
            .network("IRCNet")
            .userId("-pete@example")
            .username("_Pete_")
            .displayName("_Pete_")
            .source("IRC_TOKEN_CLAIM")
            .build()))
        .build();
  }

  private UserNotifyRule rule(String patternType, String pattern) {
    UserNotifyRule rule = new UserNotifyRule();
    rule.setEnabled(true);
    rule.setSourceEchoToAlias("ircnet-lowlife");
    rule.setSourceDisplayName("IRCNet / #lowlife");
    rule.setPatternType(patternType);
    rule.setPattern(pattern);
    rule.setDestinationConnectionType("WHATSAPP_CONNECTION");
    rule.setCooldownSeconds(60);
    return rule;
  }

  private EngineRequest request(String message, String sender) {
    return EngineRequest.builder()
        .command(message)
        .replyTo("#lowlife")
        .echoToAlias("ircnet-lowlife")
        .chatProtocol("irc")
        .network("IRCNet")
        .fromSender(sender)
        .fromSenderId(sender)
        .build();
  }

  private record Fixture(
      UserNotifyRuleService service,
      ConnectionManagerService connectionManagerService) {
  }

  private record StaticUsersService(List<DataNodeBase> users) implements UsersService {
    @Override
    public List<? extends DataNodeBase> findAll() {
      return users;
    }

    @Override
    public User getNotKnownUser() {
      return null;
    }

    @Override
    public User addChatIdentity(long userId, org.freakz.common.model.users.UserChatIdentity identity, boolean moveIfOwned) {
      return null;
    }

    @Override
    public void reloadUsers() {
    }
  }
}
