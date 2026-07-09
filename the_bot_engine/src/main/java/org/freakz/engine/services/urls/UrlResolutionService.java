package org.freakz.engine.services.urls;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.config.ConfiguredChannelResolver;
import org.freakz.engine.services.urls.resolver.UrlResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Service
public class UrlResolutionService {

  private static final Logger log = LoggerFactory.getLogger(UrlResolutionService.class);

  private final ConfiguredChannelResolver configuredChannelResolver;
  private final UrlExtractor urlExtractor;
  private final UrlSecurityValidator securityValidator;
  private final UrlResolutionFormatter formatter;
  private final UrlArchiveService archiveService;
  private final UrlResolverProperties properties;
  private final List<UrlResolver> resolvers;
  private final Cache<URI, UrlResolution> successCache;
  private final Cache<URI, Boolean> failureCache;

  public UrlResolutionService(
      ConfiguredChannelResolver configuredChannelResolver,
      UrlExtractor urlExtractor,
      UrlSecurityValidator securityValidator,
      UrlResolutionFormatter formatter,
      UrlArchiveService archiveService,
      UrlResolverProperties properties,
      List<UrlResolver> resolvers) {
    this.configuredChannelResolver = configuredChannelResolver;
    this.urlExtractor = urlExtractor;
    this.securityValidator = securityValidator;
    this.formatter = formatter;
    this.archiveService = archiveService;
    this.properties = properties;
    this.resolvers = resolvers;
    this.successCache = Caffeine.newBuilder()
        .maximumSize(properties.getCacheMaximumSize())
        .expireAfterWrite(properties.getSuccessCacheDuration())
        .build();
    this.failureCache = Caffeine.newBuilder()
        .maximumSize(properties.getCacheMaximumSize())
        .expireAfterWrite(properties.getFailureCacheDuration())
        .build();
  }

  @Async
  public void handleEngineRequest(EngineRequest request, BotEngine engine) {
    var channel = configuredChannelResolver.findByEchoToAlias(
        request.getBotConfig(), request.getEchoToAlias());
    boolean resolveUrls = channel != null && Boolean.TRUE.equals(channel.getResolveUrls());
    boolean captureUrls = channel != null && Boolean.TRUE.equals(channel.getCaptureResolvedUrls());
    if (!resolveUrls && !captureUrls) {
      return;
    }

    List<URI> urls = urlExtractor.extract(request.getCommand(), properties.getMaxUrlsPerMessage());
    for (URI url : urls) {
      if (!securityValidator.isAllowed(url)) {
        log.debug("Rejected unsafe URL: {}", url);
        continue;
      }
      Optional<UrlResolution> resolution = resolve(url);
      if (captureUrls) {
        archiveService.archive(url, resolution.orElse(null), request, channel);
      }
      if (resolveUrls) {
        resolution
            .map(formatter::format)
            .filter(reply -> !reply.isBlank())
            .ifPresent(reply -> engine.sendReplyMessage(request, reply));
      }
    }
  }

  public Optional<UrlResolution> resolve(URI uri) {
    if (!securityValidator.isAllowed(uri)) {
      log.debug("Rejected unsafe URL: {}", uri);
      return Optional.empty();
    }

    UrlResolution cached = successCache.getIfPresent(uri);
    if (cached != null) {
      return Optional.of(cached);
    }
    if (failureCache.getIfPresent(uri) != null) {
      return Optional.empty();
    }

    for (UrlResolver resolver : resolvers) {
      if (!resolver.supports(uri)) {
        continue;
      }
      try {
        Optional<UrlResolution> resolution = resolver.resolve(uri);
        if (resolution.isPresent()) {
          successCache.put(uri, resolution.get());
          return resolution;
        }
      } catch (Exception ex) {
        log.debug("URL resolver {} failed for {}", resolver.getClass().getSimpleName(), uri, ex);
      }
    }

    failureCache.put(uri, Boolean.TRUE);
    return Optional.empty();
  }
}
