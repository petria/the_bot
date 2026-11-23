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
import java.util.Arrays;

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
      requireAllowedUrl("Main instance URL", main);
    }
    if (!develop.isEmpty()) {
      requireAllowedUrl("Develop instance URL", develop);
    }

    store(MAIN_URL_KEY, main);
    store(DEVELOP_URL_KEY, develop);
    return getSettings();
  }

  private WebUiLinksSettings.WebUiLink link(String key, String defaultUrl) {
    String override = normalize(envValues.getKeyValueOrDefault(key, ""));
    if (override.isEmpty() || !isAllowed(override)) {
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
    return secureUrl(url) != null;
  }

  private String normalize(String value) {
    String normalized = value == null ? "" : value.trim();
    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private void requireAllowedUrl(String label, String url) {
    if (!isAllowed(url)) {
      throw new IllegalArgumentException(label + " must be HTTPS and use an approved host");
    }
  }

  private boolean isAllowed(String value) {
    URI url = secureUrl(value);
    if (url == null) {
      return false;
    }
    String host = url.getHost().toLowerCase(Locale.ROOT);
    URI own = secureUrl(ownPublicBaseUrl());
    if (host.equals(URI.create(DEFAULT_MAIN_URL).getHost())
        || host.equals(URI.create(DEFAULT_DEVELOP_URL).getHost())
        || (own != null && host.equalsIgnoreCase(own.getHost()))) {
      return true;
    }
    String configured = configService.getConfigValue(
        "the.bot.webui-links.allowed-hosts", "THE_BOT_WEBUI_LINKS_ALLOWED_HOSTS", "");
    return configured != null && Arrays.stream(configured.split(","))
        .map(String::strip)
        .anyMatch(host::equalsIgnoreCase);
  }

  private URI secureUrl(String value) {
    try {
      URI uri = new URI(value);
      return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getRawUserInfo() == null
          ? uri : null;
    } catch (URISyntaxException e) {
      return null;
    }
  }
}
