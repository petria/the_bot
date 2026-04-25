package org.freakz.engine.services.notifications;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.connections.ConnectionManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PrivateChatAlertService {

  private static final Logger log = LoggerFactory.getLogger(PrivateChatAlertService.class);
  private static final String ALERT_TARGETS_KEY = "channel.do.sys.notify";

  private final ConfigService configService;
  private final UsersService usersService;
  private final ConnectionManagerService connectionManagerService;
  private final Set<String> notifiedChatIds = ConcurrentHashMap.newKeySet();

  public PrivateChatAlertService(
      ConfigService configService,
      UsersService usersService,
      ConnectionManagerService connectionManagerService) {
    this.configService = configService;
    this.usersService = usersService;
    this.connectionManagerService = connectionManagerService;
  }

  public void notifyUnknownPrivateChatIfNeeded(EngineRequest request, User user) {
    if (request == null || user == null || !request.isPrivateChannel() || !isUnknownUser(user)) {
      return;
    }

    String dedupeKey = firstNonBlank(request.getChatId(), request.getChatProtocol() + ":" + request.getNetwork() + ":" + request.getFromSenderId());
    if (!notifiedChatIds.add(dedupeKey)) {
      return;
    }

    Set<String> targets = resolveAlertTargets();
    if (targets.isEmpty()) {
      log.debug("No {} configured, skipping unknown private chat alert for {}", ALERT_TARGETS_KEY, dedupeKey);
      return;
    }

    String actor = firstNonBlank(request.getFromSender(), request.getFromSenderId(), "unknown");
    String alert = String.format(
        "ALERT: unknown private chat from %s via %s/%s chatId=%s message=\"%s\"",
        actor,
        firstNonBlank(request.getChatProtocol(), "?"),
        firstNonBlank(request.getNetwork(), "?"),
        firstNonBlank(request.getChatId(), "?"),
        abbreviate(request.getCommand(), 180)
    );

    sendAlertToConfiguredTargets(alert, targets);
  }

  public Set<String> sendAlertToConfiguredTargets(String message) {
    return sendAlertToConfiguredTargets(message, resolveAlertTargets());
  }

  public Set<String> sendAlertToConfiguredTargets(String message, Set<String> targets) {
    for (String target : targets) {
      try {
        connectionManagerService.sendMessageByEchoToAlias(message, target);
      } catch (Exception e) {
        log.error("Failed to send alert to {}: {}", target, e.getMessage());
      }
    }
    return targets;
  }

  private boolean isUnknownUser(User user) {
    User notKnownUser = usersService.getNotKnownUser();
    return notKnownUser != null
        && notKnownUser.getId() != null
        && notKnownUser.getId().equals(user.getId());
  }

  private Set<String> resolveAlertTargets() {
    String raw = configService.getConfigValue(ALERT_TARGETS_KEY, null, null);
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    Set<String> targets = new LinkedHashSet<>();
    Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .forEach(targets::add);
    return targets;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private String abbreviate(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    String normalized = value.replaceAll("[\\r\\n]+", " ").trim();
    if (normalized.length() <= maxLength) {
      return normalized;
    }
    return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
  }
}
