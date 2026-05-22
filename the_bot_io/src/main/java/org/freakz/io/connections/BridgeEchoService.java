package org.freakz.io.connections;

import org.freakz.common.exception.InvalidEchoToAliasException;
import org.freakz.common.model.botconfig.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

final class BridgeEchoService {

  private static final Logger log = LoggerFactory.getLogger(BridgeEchoService.class);

  private BridgeEchoService() {
  }

  static void echoToConfiguredTargets(
      ConnectionManager connectionManager,
      Channel sourceChannel,
      String protocol,
      String actorName,
      String message,
      String botName) {
    if (connectionManager == null || sourceChannel == null) {
      return;
    }

    List<String> echoToAliases = sourceChannel.getEchoToAliases();
    if (echoToAliases == null || echoToAliases.isEmpty()) {
      return;
    }

    if (shouldSkipEcho(message, botName)) {
      return;
    }

    String sourceEchoToAlias = normalizeAlias(sourceChannel.getEchoToAlias());
    String bridgedMessage = formatBridgeMessage(protocol, actorName, message);
    for (String echoToAlias : echoToAliases) {
      String normalizedTarget = normalizeAlias(echoToAlias);
      if (normalizedTarget == null || normalizedTarget.equals(sourceEchoToAlias)) {
        log.debug("Skip echo target sourceEchoToAlias={} targetEchoToAlias={}", sourceChannel.getEchoToAlias(), echoToAlias);
        continue;
      }
      try {
        log.debug("Echo to: {}", echoToAlias);
        connectionManager.sendMessageByEchoToAlias(bridgedMessage, echoToAlias);
      } catch (InvalidEchoToAliasException e) {
        log.error("Can not echo message to: {}", echoToAlias);
      }
    }
  }

  static boolean shouldSkipEcho(String message, String botName) {
    if (message == null || message.isBlank()) {
      return true;
    }
    if (BridgeMessageGuard.shouldSkipEcho(message)) {
      log.debug("Skip bridge echo loop candidate");
      return true;
    }
    String trimmed = message.trim();
    return trimmed.startsWith("!") || startsWithBotNameCommand(trimmed, botName);
  }

  static String formatBridgeMessage(String protocol, String actorName, String message) {
    String cleanProtocol = firstNonBlank(protocol, "Unknown");
    String cleanActor = firstNonBlank(actorName, "unknown");
    return String.format("<%s@%s>: %s", cleanActor, cleanProtocol, message);
  }

  private static boolean startsWithBotNameCommand(String message, String botName) {
    if (message == null || botName == null || botName.isBlank()) {
      return false;
    }
    String cleanBotName = botName.trim();
    if (message.length() < cleanBotName.length()) {
      return false;
    }
    if (!message.regionMatches(true, 0, cleanBotName, 0, cleanBotName.length())) {
      return false;
    }
    if (message.length() == cleanBotName.length()) {
      return true;
    }
    char next = message.charAt(cleanBotName.length());
    return Character.isWhitespace(next) || next == ':';
  }

  private static String normalizeAlias(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toUpperCase();
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }
}
