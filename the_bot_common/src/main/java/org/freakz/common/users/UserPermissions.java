package org.freakz.common.users;

import org.freakz.common.model.users.User;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class UserPermissions {

  private UserPermissions() {
  }

  public static boolean has(User user, String permission) {
    if (user == null || permission == null || permission.isBlank()) {
      return false;
    }
    Set<String> normalized = normalizedSet(user.getPermissions());
    return normalized.contains(BotPermission.ALL) || normalized.contains(normalize(permission));
  }

  public static boolean hasAny(User user, Collection<String> permissions) {
    if (permissions == null || permissions.isEmpty()) {
      return true;
    }
    for (String permission : permissions) {
      if (has(user, permission)) {
        return true;
      }
    }
    return false;
  }

  public static List<String> normalize(Collection<String> permissions) {
    return normalizedSet(permissions).stream().toList();
  }

  public static List<String> effective(User user) {
    if (user == null) {
      return List.of();
    }
    Set<String> normalized = normalizedSet(user.getPermissions());
    if (normalized.contains(BotPermission.ALL)) {
      normalized.addAll(BotPermission.known());
    }
    return normalized.stream().toList();
  }

  private static Set<String> normalizedSet(Collection<String> permissions) {
    Set<String> normalized = new TreeSet<>();
    if (permissions == null) {
      return normalized;
    }
    for (String permission : permissions) {
      String value = normalize(permission);
      if (value != null) {
        normalized.add(value);
      }
    }
    return normalized;
  }

  private static String normalize(String permission) {
    if (permission == null || permission.isBlank()) {
      return null;
    }
    return permission.trim().toLowerCase(Locale.ROOT);
  }
}
