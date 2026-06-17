package org.freakz.engine.services.ai;

public final class AiReplyGuard {

  private AiReplyGuard() {
  }

  public static String stripJsonFence(String text) {
    if (text == null) {
      return "";
    }
    String cleaned = text.trim();
    if (cleaned.startsWith("```json")) {
      cleaned = cleaned.substring("```json".length()).trim();
    } else if (cleaned.startsWith("```")) {
      cleaned = cleaned.substring("```".length()).trim();
    }
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
    }
    return cleaned;
  }

  public static boolean looksLikeStructuredJson(String text) {
    if (text == null) {
      return false;
    }
    String trimmed = text.trim();
    if (trimmed.startsWith("```")) {
      return true;
    }
    String cleaned = stripJsonFence(trimmed);
    return cleaned.startsWith("{") || cleaned.startsWith("[");
  }

  public static String safeFinalAnswer(String answer, String fallback) {
    if (looksLikeStructuredJson(answer)) {
      return fallback;
    }
    return answer == null ? "" : answer;
  }

  public static String safeFailure(String prefix, String detail) {
    if (detail == null || detail.isBlank()) {
      return prefix;
    }
    if (containsJsonBody(detail)) {
      return prefix + " upstream returned a structured error.";
    }
    return prefix + " " + detail;
  }

  private static boolean containsJsonBody(String text) {
    String cleaned = stripJsonFence(text);
    return (cleaned.contains("{") && cleaned.contains("}"))
        || (cleaned.contains("[") && cleaned.contains("]"));
  }
}
