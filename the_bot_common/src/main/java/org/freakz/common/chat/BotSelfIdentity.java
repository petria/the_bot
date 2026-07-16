package org.freakz.common.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Platform identities that represent the currently connected bot account. */
public final class BotSelfIdentity {

  private final String protocol;
  private final String displayName;
  private final List<String> forms;

  public BotSelfIdentity(String protocol, String displayName, List<String> forms) {
    this.protocol = protocol;
    this.displayName = displayName;
    List<String> normalized = new ArrayList<>();
    if (forms != null) {
      forms.stream()
          .map(BotSelfIdentity::normalize)
          .filter(value -> !value.isBlank())
          .forEach(value -> {
            if (!normalized.contains(value)) {
              normalized.add(value);
            }
          });
    }
    if (displayName != null && !displayName.isBlank()) {
      String value = normalize(displayName);
      if (!normalized.contains(value)) {
        normalized.add(value);
      }
    }
    this.forms = Collections.unmodifiableList(normalized);
  }

  public String getProtocol() {
    return protocol;
  }

  public String getDisplayName() {
    return displayName;
  }

  public List<String> getForms() {
    return forms;
  }

  public boolean matches(String mention) {
    if (mention == null || mention.isBlank()) {
      return false;
    }
    String[] tokens = mention.trim().split("[\\s,;:!?()\\[\\]{}]+");
    for (String token : tokens) {
      String value = normalize(token);
      if (!value.isBlank() && forms.stream().anyMatch(form -> value.equals(form) || value.endsWith("/" + form))) {
        return true;
      }
    }
    return false;
  }

  public static String normalize(String value) {
    if (value == null) {
      return "";
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    while (normalized.startsWith("@")) {
      normalized = normalized.substring(1);
    }
    normalized = normalized.replace("<@!", "").replace("<@", "").replace(">", "");
    normalized = normalized.replace("@s.whatsapp.net", "")
        .replace("@lid", "")
        .replace("@c.us", "");
    return normalized.replaceAll(":\\d+$", "");
  }
}
