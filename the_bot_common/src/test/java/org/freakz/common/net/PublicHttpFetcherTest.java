package org.freakz.common.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class PublicHttpFetcherTest {

  @Test
  void followsRedirectWithNonHtmlContentTypeAndDropsCrossOriginCredentials() throws Exception {
    InetAddress loopback = InetAddress.getByName("127.0.0.1");
    HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
    int port = server.getAddress().getPort();
    URI initial = URI.create("http://fixture-origin.test:" + port + "/redirect");
    URI target = URI.create("http://fixture-target.test:" + port + "/target");
    AtomicReference<String> originHeader = new AtomicReference<>();
    AtomicReference<String> targetHeader = new AtomicReference<>();
    server.createContext("/redirect", exchange -> {
      originHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
      exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
      exchange.getResponseHeaders().set("Location", target.toString());
      exchange.sendResponseHeaders(302, -1);
      exchange.close();
    });
    server.createContext("/target", exchange -> {
      targetHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
      byte[] body = "<html><title>Redirect target</title></html>".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/html");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try (var policy = mockStatic(PublicUrlPolicy.class)) {
      policy.when(() -> PublicUrlPolicy.resolve(initial)).thenReturn(new InetAddress[]{loopback});
      policy.when(() -> PublicUrlPolicy.resolve(target)).thenReturn(new InetAddress[]{loopback});

      var result = PublicHttpFetcher.get(initial, Map.of("Authorization", "Bearer fixture"), 1000, 1000, 1024, 2);

      assertThat(result.uri()).isEqualTo(target);
      assertThat(result.contentType()).isEqualTo("text/html");
      assertThat(new String(result.body(), StandardCharsets.UTF_8)).contains("Redirect target");
      assertThat(originHeader.get()).isEqualTo("Bearer fixture");
      assertThat(targetHeader.get()).isNull();
      policy.verify(() -> PublicUrlPolicy.resolve(initial));
      policy.verify(() -> PublicUrlPolicy.resolve(target));
    } finally {
      server.stop(0);
    }
  }
}
