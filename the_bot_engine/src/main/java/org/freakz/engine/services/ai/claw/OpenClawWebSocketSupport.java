package org.freakz.engine.services.ai.claw;

import org.freakz.engine.config.ConfigService;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class OpenClawWebSocketSupport {

  private OpenClawWebSocketSupport() {
  }

  static String appendTokenToUrl(String url, String token) {
    if (url.contains("token=")) {
      return url;
    }
    String sep = url.contains("?") ? "&" : "?";
    return url + sep + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
  }

  static HttpHeaders createWebSocketHeaders(ConfigService configService) {
    HttpHeaders headers = new HttpHeaders();
    String origin =
        OpenClawConfigSupport.getConfigValue(
            configService,
            "openclawGatewayWsOrigin",
            "OPENCLAW_GATEWAY_WS_ORIGIN",
            "null");
    if (origin != null && !origin.isBlank() && !"none".equalsIgnoreCase(origin.trim())) {
      headers.setOrigin(origin.trim());
    }
    return headers;
  }

  static Path getStateDirPath(ConfigService configService) {
    String configured =
        OpenClawConfigSupport.getConfigValue(
            configService,
            "openclawStateDirHost",
            "OPENCLAW_STATE_DIR_HOST",
            "./openclaw/state");
    return Path.of(configured);
  }

  static JsonNode readJsonFile(JsonMapper objectMapper, Path path) throws IOException {
    if (!Files.exists(path)) {
      throw new IOException("missing file " + path);
    }
    return objectMapper.readTree(Files.readString(path));
  }
}
