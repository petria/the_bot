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
    when(securityValidator.isAllowed(uri)).thenReturn(true);
    when(resolver.supports(uri)).thenReturn(true);
    when(resolver.resolve(uri)).thenReturn(Optional.of(
        new UrlResolution(uri, "Web", "Example", null, null, null, null, null)));
    UrlResolutionService service = service(securityValidator, List.of(resolver));
    BotEngine engine = mock(BotEngine.class);
    EngineRequest request = request(true);

    service.handleEngineRequest(request, engine);

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
    UrlResolutionService service = service(securityValidator, List.of(resolver));

    service.resolve(uri);
    service.resolve(uri);

    verify(resolver).resolve(uri);
  }

  @Test
  void skipsUrlWhenConfiguredChannelDoesNotAllowIt() {
    UrlResolver resolver = mock(UrlResolver.class);
    UrlSecurityValidator securityValidator = mock(UrlSecurityValidator.class);
    UrlResolutionService service = service(securityValidator, List.of(resolver));
    BotEngine engine = mock(BotEngine.class);
    EngineRequest request = request(false);

    service.handleEngineRequest(request, engine);

    verify(resolver, never()).resolve(URI.create("https://example.com"));
    verify(engine, never()).sendReplyMessage(request, "[ \u0002Example\u0002 ]");
  }

  private UrlResolutionService service(
      UrlSecurityValidator securityValidator,
      List<UrlResolver> resolvers) {
    return new UrlResolutionService(
        new ConfiguredChannelResolver(),
        new UrlExtractor(),
        securityValidator,
        new UrlResolutionFormatter(),
        new UrlResolverProperties(),
        resolvers);
  }

  private EngineRequest request(boolean resolveUrls) {
    Channel channel = Channel.builder()
        .echoToAlias("DISCORD-GENERAL")
        .resolveUrls(resolveUrls)
        .build();
    TheBotConfig config = TheBotConfig.builder()
        .discordConfig(DiscordConfig.builder().channelList(List.of(channel)).build())
        .build();
    return EngineRequest.builder()
        .command("See https://example.com")
        .echoToAlias("DISCORD-GENERAL")
        .botConfig(config)
        .build();
  }
}
