package org.freakz.common.util;

import java.util.Locale;

public final class TextUtils {

  private TextUtils() {
  }

  public static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  public static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  public static String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  public static String trimToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  public static String lowerTrimToNull(String value) {
    String trimmed = trimToNull(value);
    return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
  }

  public static String lowerTrimToEmpty(String value) {
    return trimToEmpty(value).toLowerCase(Locale.ROOT);
  }

  public static String collapseWhitespace(String value) {
    return value == null ? "" : value.replaceAll("\\s+", " ").trim();
  }

  public static String abbreviate(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    String normalized = collapseWhitespace(value);
    if (normalized.length() <= maxLength) {
      return normalized;
    }
    return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
  }
}
