package org.freakz.common.irc;

import java.util.stream.Collectors;

/** Normalized parameterless IRC channel modes guarded by bot configuration. */
public record IrcChannelModeSpec(String value) {

  public IrcChannelModeSpec {
    value = value == null ? "" : value;
  }

  public static IrcChannelModeSpec parse(String raw) {
    if (raw == null || raw.isBlank()) {
      return new IrcChannelModeSpec("");
    }
    String value = raw.trim();
    if (!value.matches("\\+[A-Za-z]+")) {
      throw new IllegalArgumentException("IRC channel modes must be parameterless flags such as +st");
    }
    String modes = value.substring(1).chars()
        .mapToObj(character -> String.valueOf((char) character))
        .distinct()
        .sorted()
        .collect(Collectors.joining());
    return new IrcChannelModeSpec("+" + modes);
  }

  public boolean contains(char mode) {
    return value.indexOf(mode) >= 0;
  }

  public boolean isEmpty() {
    return value.isEmpty();
  }
}
