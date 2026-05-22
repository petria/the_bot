package org.freakz.engine.services.ai.claw;

import org.freakz.common.model.engine.system.OpenClawInstanceOption;
import org.freakz.common.model.engine.system.OpenClawSettingsResponse;
import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpenClawInstanceSettingsService {

  private static final String WS_URL_KEY = "openclawGatewayWsUrl";
  private static final String ORIGIN_URL_KEY = "openclawGatewayWsOrigin";

  private final ConfigService configService;
  private final EnvValuesService envValuesService;

  public OpenClawInstanceSettingsService(ConfigService configService, EnvValuesService envValuesService) {
    this.configService = configService;
    this.envValuesService = envValuesService;
  }

  public OpenClawSettingsResponse getSettings() {
    String currentWsUrl = configService.getConfigValue(WS_URL_KEY, "OPENCLAW_GATEWAY_WS_URL", "ws://ubuntu-server.local:18889");
    String currentOriginUrl = configService.getConfigValue(ORIGIN_URL_KEY, "OPENCLAW_GATEWAY_WS_ORIGIN", originFromWsUrl(currentWsUrl));
    String currentInstanceId = instanceIdForWsUrl(currentWsUrl);
    return response(currentInstanceId, currentWsUrl, currentOriginUrl);
  }

  public OpenClawSettingsResponse selectInstance(String selectedInstanceId) {
    OpenClawInstanceOption selected = options(null).stream()
        .filter(option -> option.id().equals(selectedInstanceId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported OpenClaw instance: " + selectedInstanceId));

    User user = User.builder().username("bot-web").name("bot-web").build();
    envValuesService.setEnvValue(WS_URL_KEY, selected.wsUrl(), user);
    envValuesService.setEnvValue(ORIGIN_URL_KEY, selected.originUrl(), user);
    return response(selected.id(), selected.wsUrl(), selected.originUrl());
  }

  private OpenClawSettingsResponse response(String currentInstanceId, String currentWsUrl, String currentOriginUrl) {
    String healthUrl = healthUrlFromWsUrl(currentWsUrl);
    return new OpenClawSettingsResponse(
        currentInstanceId,
        currentWsUrl,
        currentOriginUrl,
        healthUrl,
        options(currentInstanceId));
  }

  private List<OpenClawInstanceOption> options(String selectedId) {
    return List.of(
        option("ubuntu-server.local", "ubuntu-server.local", selectedId),
        option("docker.local", "docker.local", selectedId));
  }

  private OpenClawInstanceOption option(String id, String host, String selectedId) {
    String wsUrl = "ws://" + host + ":18889";
    String originUrl = "http://" + host + ":18889";
    return new OpenClawInstanceOption(
        id,
        host,
        host,
        wsUrl,
        originUrl,
        originUrl + "/health",
        id.equals(selectedId));
  }

  private String instanceIdForWsUrl(String wsUrl) {
    if (wsUrl == null) {
      return null;
    }
    return options(null).stream()
        .filter(option -> option.wsUrl().equalsIgnoreCase(wsUrl.trim()))
        .map(OpenClawInstanceOption::id)
        .findFirst()
        .orElse(null);
  }

  private String originFromWsUrl(String wsUrl) {
    if (wsUrl == null || wsUrl.isBlank()) {
      return "http://ubuntu-server.local:18889";
    }
    return wsUrl.trim()
        .replaceFirst("^wss://", "https://")
        .replaceFirst("^ws://", "http://");
  }

  private String healthUrlFromWsUrl(String wsUrl) {
    return originFromWsUrl(wsUrl) + "/health";
  }
}
