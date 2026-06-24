package org.freakz.engine.services.notifications;

import org.freakz.common.model.connectionmanager.SendMessageToKnownUserResponse;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.notify.UserNotifyRule;
import org.freakz.common.model.engine.notify.UserNotifyRuleListResponse;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;
import org.freakz.common.users.UserChatIdentityUtil;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.connections.ConnectionManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

@Service
public class UserNotifyRuleService {

  private static final Logger log = LoggerFactory.getLogger(UserNotifyRuleService.class);
  private static final int MAX_NOTIFICATION_MESSAGE_LENGTH = 420;

  private final UserNotifyRuleStore store;
  private final UsersService usersService;
  private final ConnectionManagerService connectionManagerService;
  private final Map<String, Long> lastSentByRuleId = new ConcurrentHashMap<>();

  @Autowired
  public UserNotifyRuleService(
      ConfigService configService,
      JsonMapper jsonMapper,
      UsersService usersService,
      ConnectionManagerService connectionManagerService) {
    this.store = new UserNotifyRuleStore(
        configService.getRuntimeDataFile(UserNotifyRuleStore.USER_NOTIFY_RULES_FILE).toPath(),
        jsonMapper);
    this.usersService = usersService;
    this.connectionManagerService = connectionManagerService;
  }

  UserNotifyRuleService(
      UserNotifyRuleStore store,
      UsersService usersService,
      ConnectionManagerService connectionManagerService) {
    this.store = store;
    this.usersService = usersService;
    this.connectionManagerService = connectionManagerService;
  }

  public UserNotifyRuleListResponse list(String username) {
    return new UserNotifyRuleListResponse(store.findByUsername(username));
  }

  public UserNotifyRule create(String username, UserNotifyRule rule) {
    return store.create(username, rule);
  }

  public UserNotifyRule update(String username, String id, UserNotifyRule rule) {
    return store.update(username, id, rule);
  }

  public void delete(String username, String id) {
    store.delete(username, id);
    lastSentByRuleId.remove(id);
  }

  public void processInboundMessage(EngineRequest request) {
    if (request == null || request.isPrivateChannel()) {
      return;
    }
    String message = clean(request.getMessage());
    String sourceEchoToAlias = clean(request.getEchoToAlias());
    if (message == null || sourceEchoToAlias == null) {
      return;
    }

    for (UserNotifyRule rule : store.findEnabled()) {
      try {
        processRule(request, message, sourceEchoToAlias, rule);
      } catch (RuntimeException e) {
        log.warn("User notify rule {} failed: {}", rule.getId(), e.getMessage());
        log.debug("User notify rule failure", e);
      }
    }
  }

  private void processRule(
      EngineRequest request,
      String message,
      String sourceEchoToAlias,
      UserNotifyRule rule) {
    if (!sameText(sourceEchoToAlias, rule.getSourceEchoToAlias())) {
      return;
    }
    User owner = findUser(rule.getUsername());
    if (owner == null || senderMatchesUser(owner, request)) {
      return;
    }
    if (!matches(rule, owner, message)) {
      return;
    }
    if (cooldownActive(rule)) {
      return;
    }

    SendMessageToKnownUserResponse response = connectionManagerService.sendMessageToKnownUser(
        rule.getUsername(),
        notificationMessage(rule, request, message),
        true,
        rule.getDestinationConnectionType(),
        null);
    if (response == null || !"OK".equals(response.getStatus())) {
      log.warn(
          "User notify rule {} matched but delivery failed for user {}: {}",
          rule.getId(),
          rule.getUsername(),
          response == null ? "no response" : response.getMessage());
      return;
    }
    lastSentByRuleId.put(rule.getId(), System.currentTimeMillis());
  }

  private boolean matches(UserNotifyRule rule, User owner, String message) {
    if (UserNotifyRule.PATTERN_TYPE_REGEX.equals(rule.getPatternType())) {
      try {
        return Pattern.compile(rule.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
            .matcher(message)
            .find();
      } catch (PatternSyntaxException e) {
        return false;
      }
    }
    return mentionNames(owner).stream().anyMatch(name -> mentionPattern(name).matcher(message).find());
  }

  private Pattern mentionPattern(String name) {
    return Pattern.compile(
        "(?iu)(^|[^\\p{L}\\p{N}_])@?" + Pattern.quote(name) + "\\s*:",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
  }

  private List<String> mentionNames(User user) {
    return Stream.of(user.getIrcNick(), user.getUsername(), user.getName())
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .distinct()
        .toList();
  }

  private boolean cooldownActive(UserNotifyRule rule) {
    int cooldownSeconds = Math.max(0, rule.getCooldownSeconds());
    if (cooldownSeconds == 0) {
      return false;
    }
    Long lastSentAt = lastSentByRuleId.get(rule.getId());
    return lastSentAt != null && System.currentTimeMillis() - lastSentAt < cooldownSeconds * 1000L;
  }

  private String notificationMessage(UserNotifyRule rule, EngineRequest request, String message) {
    String source = firstNonBlank(rule.getSourceDisplayName(), request.getReplyTo(), request.getEchoToAlias(), "channel");
    String sender = firstNonBlank(request.getFromSender(), request.getFromSenderId(), "unknown");
    return abbreviate("Notify match in " + source + " from " + sender + ": " + message, MAX_NOTIFICATION_MESSAGE_LENGTH);
  }

  private User findUser(String username) {
    String normalizedUsername = normalize(username);
    if (normalizedUsername == null) {
      return null;
    }
    for (DataNodeBase node : usersService.findAll()) {
      if (node instanceof User user && normalizedUsername.equals(normalize(user.getUsername()))) {
        return user;
      }
    }
    return null;
  }

  private boolean senderMatchesUser(User user, EngineRequest request) {
    if (user == null || request == null) {
      return false;
    }
    if (matchesAny(request.getFromSender(), user.getUsername(), user.getName(), user.getIrcNick())) {
      return true;
    }
    if (matchesAny(request.getFromSenderId(), user.getIrcNick())) {
      return true;
    }
    String connectionType = connectionType(request.getChatProtocol());
    if (connectionType == null) {
      return false;
    }
    if (UserChatIdentityUtil.matches(
        user,
        connectionType,
        request.getNetwork(),
        request.getFromSenderId(),
        request.getFromSender(),
        request.getFromSender())) {
      return true;
    }
    for (UserChatIdentity identity : UserChatIdentityUtil.normalizedIdentities(user)) {
      if (UserChatIdentityUtil.matches(
          identity,
          connectionType,
          request.getNetwork(),
          request.getFromSenderId(),
          request.getFromSender(),
          request.getFromSender())) {
        return true;
      }
    }
    return false;
  }

  private String connectionType(String protocol) {
    String normalized = normalize(protocol);
    if (normalized == null) {
      return null;
    }
    return switch (normalized) {
      case "irc" -> "IRC_CONNECTION";
      case "discord" -> "DISCORD_CONNECTION";
      case "telegram" -> "TELEGRAM_CONNECTION";
      case "whatsapp" -> "WHATSAPP_CONNECTION";
      default -> null;
    };
  }

  private boolean matchesAny(String actual, String... expectedValues) {
    String normalizedActual = normalize(actual);
    if (normalizedActual == null) {
      return false;
    }
    for (String expected : expectedValues) {
      if (normalizedActual.equals(normalize(expected))) {
        return true;
      }
    }
    return false;
  }

  private boolean sameText(String left, String right) {
    String normalizedLeft = normalize(left);
    String normalizedRight = normalize(right);
    return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
  }

  private String normalize(String value) {
    String cleaned = clean(value);
    return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      String cleaned = clean(value);
      if (cleaned != null) {
        return cleaned;
      }
    }
    return null;
  }

  private String abbreviate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, Math.max(0, maxLength - 3)) + "...";
  }
}
