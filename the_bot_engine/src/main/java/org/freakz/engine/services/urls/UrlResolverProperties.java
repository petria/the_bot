package org.freakz.engine.services.urls;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "the.bot.urls")
public class UrlResolverProperties {

  private int maxUrlsPerMessage = 2;
  private int maxResponseBytes = 1_048_576;
  private int maxRedirects = 3;
  private int connectTimeoutMillis = 3_000;
  private int readTimeoutMillis = 5_000;
  private int cacheMaximumSize = 5_000;
  private Duration successCacheDuration = Duration.ofHours(6);
  private Duration failureCacheDuration = Duration.ofMinutes(5);
  private String userAgent = "HokanTheBot URL resolver";
  private Youtube youtube = new Youtube();
  private Wikipedia wikipedia = new Wikipedia();
  private Nettiauto nettiauto = new Nettiauto();

  public int getMaxUrlsPerMessage() {
    return maxUrlsPerMessage;
  }

  public void setMaxUrlsPerMessage(int maxUrlsPerMessage) {
    this.maxUrlsPerMessage = maxUrlsPerMessage;
  }

  public int getMaxResponseBytes() {
    return maxResponseBytes;
  }

  public void setMaxResponseBytes(int maxResponseBytes) {
    this.maxResponseBytes = maxResponseBytes;
  }

  public int getMaxRedirects() {
    return maxRedirects;
  }

  public void setMaxRedirects(int maxRedirects) {
    this.maxRedirects = maxRedirects;
  }

  public int getConnectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  public void setConnectTimeoutMillis(int connectTimeoutMillis) {
    this.connectTimeoutMillis = connectTimeoutMillis;
  }

  public int getReadTimeoutMillis() {
    return readTimeoutMillis;
  }

  public void setReadTimeoutMillis(int readTimeoutMillis) {
    this.readTimeoutMillis = readTimeoutMillis;
  }

  public int getCacheMaximumSize() {
    return cacheMaximumSize;
  }

  public void setCacheMaximumSize(int cacheMaximumSize) {
    this.cacheMaximumSize = cacheMaximumSize;
  }

  public Duration getSuccessCacheDuration() {
    return successCacheDuration;
  }

  public void setSuccessCacheDuration(Duration successCacheDuration) {
    this.successCacheDuration = successCacheDuration;
  }

  public Duration getFailureCacheDuration() {
    return failureCacheDuration;
  }

  public void setFailureCacheDuration(Duration failureCacheDuration) {
    this.failureCacheDuration = failureCacheDuration;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public Youtube getYoutube() {
    return youtube;
  }

  public void setYoutube(Youtube youtube) {
    this.youtube = youtube;
  }

  public Wikipedia getWikipedia() {
    return wikipedia;
  }

  public void setWikipedia(Wikipedia wikipedia) {
    this.wikipedia = wikipedia;
  }

  public Nettiauto getNettiauto() {
    return nettiauto;
  }

  public void setNettiauto(Nettiauto nettiauto) {
    this.nettiauto = nettiauto;
  }

  public static class Youtube {

    private boolean enabled = true;
    private String apiKey;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }
  }

  public static class Wikipedia {

    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static class Nettiauto {

    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
