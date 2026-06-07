package org.freakz.engine.services.urls.resolver;

import com.sun.net.httpserver.HttpServer;
import org.freakz.engine.services.urls.UrlResolution;
import org.freakz.engine.services.urls.UrlResolverProperties;
import org.freakz.engine.services.urls.UrlSecurityValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenericHtmlUrlResolverTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void prefersOpenGraphMetadataOverHtmlTitle() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      byte[] body = """
          <html>
            <head>
              <title>Fallback title</title>
              <meta property="og:site_name" content="Test Provider">
              <meta property="og:title" content="OpenGraph title">
              <meta property="og:description" content="Description">
              <meta name="author" content="Author">
            </head>
          </html>
          """.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    UrlSecurityValidator securityValidator = mock(UrlSecurityValidator.class);
    when(securityValidator.isAllowed(any())).thenReturn(true);
    GenericHtmlUrlResolver resolver = new GenericHtmlUrlResolver(
        new UrlResolverProperties(), securityValidator);

    Optional<UrlResolution> result = resolver.resolve(uri);

    assertThat(result).hasValueSatisfying(resolution -> {
      assertThat(resolution.provider()).isEqualTo("Test Provider");
      assertThat(resolution.title()).isEqualTo("OpenGraph title");
      assertThat(resolution.description()).isEqualTo("Description");
      assertThat(resolution.author()).isEqualTo("Author");
    });
  }
}
