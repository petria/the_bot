package org.freakz.common.users;

import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;

import java.util.List;

public final class UserChatIdentityUtil {

  private UserChatIdentityUtil() {
  }

  public static String identityKey(UserChatIdentity identity) {
    if (identity == null) {
      return null;
    }
    return identityKey(
        identity.getConnectionType(),
        identity.getNetwork(),
        identity.getUserId(),
        identity.getUsername(),
        identity.getDisplayName());
  }

  public static String identityKey(
      String connectionType,
      String network,
      String userId,
      String username,
      String displayName) {
    String normalizedConnectionType = normalize(connectionType);
    if (normalizedConnectionType == null) {
      return null;
    }
    String stableUserId = normalizeComparable(userId);
    String stableUsername = normalizeComparable(username);
    String stableDisplayName = normalizeComparable(displayName);
    String identityValue = stableUserId != null ? "id:" + stableUserId
        : stableUsername != null ? "username:" + stableUsername
        : stableDisplayName != null ? "display:" + stableDisplayName
        : null;
    if (identityValue == null) {
      return null;
    }
    return String.join("|", normalizedConnectionType, normalize(network) == null ? "" : normalize(network), identityValue);
  }

  public static boolean matches(
      User user,
      String connectionType,
      String network,
      String userId,
      String username,
      String displayName) {
    if (user == null || user.getChatIdentities() == null) {
      return false;
    }
    for (UserChatIdentity identity : user.getChatIdentities()) {
      if (matches(identity, connectionType, network, userId, username, displayName)) {
        return true;
      }
    }
    return false;
  }

  public static boolean matches(
      UserChatIdentity identity,
      String connectionType,
      String network,
      String userId,
      String username,
      String displayName) {
    if (identity == null || !sameNormalized(identity.getConnectionType(), connectionType)) {
      return false;
    }
    String configuredNetwork = normalize(identity.getNetwork());
    String observedNetwork = normalize(network);
    if (configuredNetwork != null && observedNetwork != null && !configuredNetwork.equals(observedNetwork)) {
      return false;
    }
    if (configuredValueMatchesObserved(identity.getUserId(), userId)) {
      return true;
    }
    if (configuredValueMatchesObserved(identity.getUsername(), username)) {
      return true;
    }
    return configuredValueMatchesObserved(identity.getDisplayName(), displayName);
  }

  public static boolean configuredValueMatchesObserved(String configuredValue, String... observedValues) {
    String normalizedConfigured = normalizeComparable(configuredValue);
    if (normalizedConfigured == null) {
      return false;
    }
    for (String observedValue : observedValues) {
      String normalizedObserved = normalizeComparable(observedValue);
      if (normalizedObserved != null && normalizedConfigured.equals(normalizedObserved)) {
        return true;
      }
    }
    return false;
  }

  public static List<UserChatIdentity> normalizedIdentities(User user) {
    return user == null || user.getChatIdentities() == null ? List.of() : user.getChatIdentities();
  }

  public static String normalizeComparable(String value) {
    String normalized = normalize(value);
    if (normalized == null || "none".equals(normalized) || "null".equals(normalized)) {
      return null;
    }
    return normalized;
  }

  public static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim().toLowerCase();
  }

  private static boolean sameNormalized(String left, String right) {
    String normalizedLeft = normalize(left);
    String normalizedRight = normalize(right);
    return normalizedLeft != null && normalizedLeft.equals(normalizedRight);
  }
}
