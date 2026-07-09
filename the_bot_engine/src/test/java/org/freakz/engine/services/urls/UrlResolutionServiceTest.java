package org.freakz.engine.services.urls;

import org.freakz.common.model.botconfig.Channel;
import org.freakz.common.model.botconfig.DiscordConfig;
import org.freakz.common.model.botconfig.TheBotConfig;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.config.ConfiguredChannelResolver;
import org.freakz.engine.services.urls.resolver.UrlResolver;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UrlResolutionServiceTest {

  @Test
  void resolvesUrlWhenConfiguredChannelAllowsIt() {
    URI uri = URI.create("https://example.com");
    UrlResolver resolver = mock(UrlResolver.class);
    UrlSecurityValidator securityValidator = mock(UrlSecurityValidator.class);
    UrlArchiveService archiveService = mock(UrlArchiveService.class);
    when(securityValidator.isAllowed(uri)).thenReturn(true);
    when(resolver.supports(uri)).thenReturn(true);
    UrlResolution resolution = new UrlResolution(uri, "Web", "Example", null, null, null, null, null);
    when(resolver.resolve(uri)).thenReturn(Optional.of(resolution));
    UrlResolutionService service = service(securityValidator, archiveService, List.of(resolver));
    BotEngine engine = mock(BotEngine.class);
    EngineRequest request = request(true, false);

    service.handleEngineRequest(request, engine);

    verify(engine).sendReplyMessage(request, "[ \u0002Example\u0002 ]");
    verify(archiveService, never()).archive(any(URI.class), any(), any(), any());
  }

  @Test
  void archivesUrlWithResolvedMetadataWhenConfiguredChannelCapturesUrls() {
    URI uri = URI.create("https://example.com");
    UrlResolver resolver = mock(UrlResolver.class);
    UrlSecurityValidator securityValidator = mock(UrlSecurityValidator.class);
    UrlArchiveService archiveService = mock(UrlArchiveService.class);
    when(securityValidator.isAllowed(uri)).thenReturn(true);
    when(resolver.supports(uri)).thenReturn(true);
    UrlResolution resolution = new UrlResolution(uri, "Web", "Example", null, null, null, null, null);
    when(resolver.resolve(uri)).thenReturn(Optional.of(resolution));
    UrlResolutionService service = service(securityValidator, archiveService, List.of(resolver));
    BotEngine engine = mock(BotEngine.class);
    EngineRequest request = request(false, true);

    service.handleEngineRequest(request, engine);

    verify(archiveService).archive(uri, resolution, request, channel(request));
    verify(engine, never()).sendReplyMessage(request, "[ \u0002Example\u0002 ]");
  }

  @Test
  void archivesRawUrlWhenConfiguredChannelCapturesUrlsAndMetadataResolutionFails() {
    URI uri = URI.create("https://example.com/image.jpg");
    UrlResolver resolver = mock(UrlResolver.class);
    UrlSecurityValidator securityValidator = mock(UrlSecurityValidator.class);
    UrlArchiveService archiveService = mock(UrlArchiveService.class);
    when(securityValidator.isAllowed(uri)).thenReturn(true);
    when(resolver.supports(uri)).thenReturn(true);
    when(resolver.resolve(uri)).thenReturn(Optional.empty());
    UrlResolutionService service = service(securityValidator, archiveService, List.of(resolver));
    BotEngine engine = mock(BotEngine.class);
    EngineRequest request = request(false, true, "See https://example.com/image.jpg");

    service.handleEngineRequest(request, engine);

    verify(archiveService).archive(uri, null, request, channel(request));
    verify(engine, never()).sendReplyMessage(request, "[ \u0002Example\u0002 ]");
  }

  @Test
  void repliesAndArchivesWhenBothChannelFlagsAreEnabled() {
    URI uri = URI.create("https://example.com");
    UrlResolver resolver = mock(UrlResolver.class);
    UrlSecurityValidator securityValidator = mock(UrlSecurityValidator.class);
    UrlArchiveService archiveService = mock(UrlArchiveService.class);
    when(securityValidator.isAllowed(uri)).thenReturn(true);
    when(resolver.supports(uri)).thenReturn(true);
    UrlResolution resolution = new UrlResolution(uri, "Web", "Example", null, null, null, null, null);
    when(resolver.resolve(uri)).thenReturn(Optional.of(resolution));
    UrlResolutionService service = service(securityValidator, archiveService, List.of(resolver));
    BotEngine engine = mock(BotEngine.class);
    EngineRequest request = request(true, true);

    service.handleEngineRequest(request, engine);

    verify(archiveService).archive(uri, resolution, request, channel(request));
    verify(engine).sendReplyMessage(request, "[ \u0002Example\u0002 ]");
  }

  @Test
  void cachesSuccessfulResolution() {
    URI uri = URI.create("https://example.com");
    UrlResolver resolver = mock(UrlResolver.class);
    UrlSecurityValidator securityValidator = mock(UrlSecurityValidator.class);
    when(securityValidator.isAllowed(uri)).thenReturn(true);
    when(resolver.supports(uri)).thenReturn(true);
    when(resolver.resolve(uri)).thenReturn(Optional.of(
        new UrlResolution(uri, "Web", "Example", null, null, null, null, null)));
    UrlResolutionService service = service(securityValidator, mock(UrlArchiveService.class), List.of(resolver));

    service.resolve(uri);
    service.resolve(uri);

    verify(resolver).resolve(uri);
  }

  @Test
  void skipsUrlWhenConfiguredChannelDoesNotAllowIt() {
    UrlResolver resolver = mock(UrlResolver.class);
    UrlSecurityValidator securityValidator = mock(UrlSecurityValidator.class);
    UrlArchiveService archiveService = mock(UrlArchiveService.class);
    UrlResolutionService service = service(securityValidator, archiveService, List.of(resolver));
    BotEngine engine = mock(BotEngine.class);
    EngineRequest request = request(false, false);

    service.handleEngineRequest(request, engine);

    verify(resolver, never()).resolve(URI.create("https://example.com"));
    verify(archiveService, never()).archive(any(URI.class), any(), any(), any());
    verify(engine, never()).sendReplyMessage(request, "[ \u0002Example\u0002 ]");
  }

  @Test
  void doesNotArchiveUnsafeUrl() {
    URI uri = URI.create("https://example.com");
    UrlResolver resolver = mock(UrlResolver.class);
    UrlSecurityValidator securityValidator = mock(UrlSecurityValidator.class);
    UrlArchiveService archiveService = mock(UrlArchiveService.class);
    when(securityValidator.isAllowed(uri)).thenReturn(false);
    UrlResolutionService service = service(securityValidator, archiveService, List.of(resolver));
    BotEngine engine = mock(BotEngine.class);
    EngineRequest request = request(false, true);

    service.handleEngineRequest(request, engine);

    verify(resolver, never()).resolve(uri);
    verify(archiveService, never()).archive(any(URI.class), any(), any(), any());
    verify(engine, never()).sendReplyMessage(request, "[ \u0002Example\u0002 ]");
  }

  private UrlResolutionService service(
      UrlSecurityValidator securityValidator,
      UrlArchiveService archiveService,
      List<UrlResolver> resolvers) {
    return new UrlResolutionService(
        new ConfiguredChannelResolver(),
        new UrlExtractor(),
        securityValidator,
        new UrlResolutionFormatter(),
        archiveService,
        new UrlResolverProperties(),
        resolvers);
  }

  private EngineRequest request(boolean resolveUrls, boolean captureResolvedUrls) {
    return request(resolveUrls, captureResolvedUrls, "See https://example.com");
  }

  private EngineRequest request(boolean resolveUrls, boolean captureResolvedUrls, String command) {
    Channel channel = Channel.builder()
        .echoToAlias("DISCORD-GENERAL")
        .resolveUrls(resolveUrls)
        .captureResolvedUrls(captureResolvedUrls)
        .build();
    TheBotConfig config = TheBotConfig.builder()
        .discordConfig(DiscordConfig.builder().channelList(List.of(channel)).build())
        .build();
    return EngineRequest.builder()
        .command(command)
        .echoToAlias("DISCORD-GENERAL")
        .botConfig(config)
        .build();
  }

  private Channel channel(EngineRequest request) {
    return request.getBotConfig().getDiscordConfig().getChannelList().getFirst();
  }
}
