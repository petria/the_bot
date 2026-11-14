package org.freakz.engine.services.webui;

import org.freakz.common.model.engine.system.WebUiLinksSettings;
import org.freakz.common.model.engine.system.WebUiLinksUpdateRequest;
import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.EnvValuesService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Web UI links for the main and develop bot instances, stored in the shared runtime settings store
 * so the values can be changed from the bot web UI without a redeploy of any client.
 */
@Service
public class WebUiLinksSettingsService {

  static final String MAIN_URL_KEY = "webui.url.main";
  static final String DEVELOP_URL_KEY = "webui.url.develop";

  static final String DEFAULT_MAIN_URL = "https://hokanthebot.airiot.fi";
  static final String DEFAULT_DEVELOP_URL = "https://hokandevbot.airiot.fi";

  private static final String MAIN_INSTANCE_SUFFIX = "-main";
  private static final User OPERATOR = User.builder().username("bot-web").name("bot-web").build();

  private final EnvValuesService envValues;
  private final ConfigService configService;

  public WebUiLinksSettingsService(EnvValuesService envValues, ConfigService configService) {
    this.envValues = envValues;
    this.configService = configService;
  }

  public WebUiLinksSettings getSettings() {
    return new WebUiLinksSettings(
        link(MAIN_URL_KEY, defaultMainUrl()),
        link(DEVELOP_URL_KEY, defaultDevelopUrl()));
  }

  public WebUiLinksSettings update(WebUiLinksUpdateRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Web UI links are required");
    }

    String main = normalize(request.mainUrl());
    String develop = normalize(request.developUrl());
    if (!main.isEmpty()) {
      requireHttpUrl("Main instance URL", main);
    }
    if (!develop.isEmpty()) {
      requireHttpUrl("Develop instance URL", develop);
    }

    store(MAIN_URL_KEY, main);
    store(DEVELOP_URL_KEY, develop);
    return getSettings();
  }

  private WebUiLinksSettings.WebUiLink link(String key, String defaultUrl) {
    String override = normalize(envValues.getKeyValueOrDefault(key, ""));
    if (override.isEmpty()) {
      return new WebUiLinksSettings.WebUiLink(defaultUrl, false);
    }
    return new WebUiLinksSettings.WebUiLink(override, true);
  }

  private void store(String key, String value) {
    if (value.isEmpty()) {
      envValues.unSetEnvValue(key, OPERATOR);
      return;
    }
    envValues.setEnvValue(key, value, OPERATOR);
  }

  private String defaultMainUrl() {
    String ownBaseUrl = ownPublicBaseUrl();
    return isMainInstance() && isPublicHttps(ownBaseUrl) ? ownBaseUrl : DEFAULT_MAIN_URL;
  }

  private String defaultDevelopUrl() {
    String ownBaseUrl = ownPublicBaseUrl();
    return !isMainInstance() && isPublicHttps(ownBaseUrl) ? ownBaseUrl : DEFAULT_DEVELOP_URL;
  }

  private String ownPublicBaseUrl() {
    return normalize(configService.getConfigValue(
        "the.bot.webPublicBaseUrl",
        "THE_BOT_WEB_PUBLIC_BASE_URL",
        "http://localhost:8091"));
  }

  private boolean isMainInstance() {
    String instanceId = configService.getConfigValue(
        "hokan.bot.instance-id",
        "HOKAN_BOT_INSTANCE_ID",
        "dev");
    return instanceId != null
        && instanceId.trim().toLowerCase(Locale.ROOT).endsWith(MAIN_INSTANCE_SUFFIX);
  }

  private boolean isPublicHttps(String url) {
    return url.toLowerCase(Locale.ROOT).startsWith("https://");
  }

  private String normalize(String value) {
    String normalized = value == null ? "" : value.trim();
    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private void requireHttpUrl(String label, String url) {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(label + " must be a valid URL: " + e.getMessage());
    }

    String scheme = uri.getScheme();
    boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    if (!httpScheme || uri.getHost() == null || uri.getHost().isBlank()) {
      throw new IllegalArgumentException(label + " must be an absolute http or https URL");
    }
  }
}
