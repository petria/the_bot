package org.freakz.engine.services.generated;

import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.services.api.ServiceRequest;

import java.util.Locale;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_CHANNEL;

final class GeneratedPageServiceSupport {

  private GeneratedPageServiceSupport() {
  }

  static String resolveChannel(ServiceRequest request) {
    EngineRequest engineRequest = request.getEngineRequest();
    if (engineRequest.isPrivateChannel()) {
      return "#amigafin";
    }
    String channel = request.getResults().getString(ARG_CHANNEL, engineRequest.getReplyTo());
    return normalize(channel);
  }

  static String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  static int parseCount(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  static String buildUrl(String baseUrl, String id, String token) {
    String trimmed = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8091" : baseUrl.trim();
    if (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed + "/generated/" + id + "?token=" + token;
  }
}
