package org.freakz.io.connections;

import java.util.regex.Pattern;

final class BridgeMessageGuard {

  private static final Pattern BRIDGE_PREFIX = Pattern.compile(
      "^[\\u0002\\s]*<[^>]+@(IRC|Discord|Dicord|Telegram|WhatsApp)>:\\s.*",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  private BridgeMessageGuard() {
  }

  static boolean shouldSkipEcho(String message) {
    if (message == null) {
      return false;
    }
    return BRIDGE_PREFIX.matcher(message).matches();
  }
}
