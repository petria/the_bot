package org.freakz.common.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.util.Timeout;

/** Fetches public URLs using the validated addresses and the original hostname for TLS. */
public final class PublicHttpFetcher {

  private PublicHttpFetcher() {}

  public static Result get(URI uri, Map<String, String> headers, int connectMillis, int readMillis,
      int maxBytes, int maxRedirects) throws IOException {
    for (int redirects = 0; ; redirects++) {
      Result response = fetchOnce(uri, headers, connectMillis, readMillis, maxBytes, true);
      if (response.location() == null) {
        return response;
      }
      if (redirects >= maxRedirects) {
        throw new IOException("Too many HTTP redirects");
      }
      URI next = uri.resolve(response.location());
      if (!sameOrigin(uri, next)) {
        headers = Map.of();
      }
      uri = next;
    }
  }

  /** Uses an operator-configured service endpoint. The caller must construct the URL itself. */
  public static Result getConfiguredService(URI uri, Map<String, String> headers, int connectMillis,
      int readMillis, int maxBytes) throws IOException {
    Result result = fetchOnce(uri, headers, connectMillis, readMillis, maxBytes, false);
    if (result.location() != null) {
      throw new IOException("Configured service attempted a redirect");
    }
    return result;
  }

  private static Result fetchOnce(URI uri, Map<String, String> headers, int connectMillis, int readMillis,
      int maxBytes, boolean requirePublic) throws IOException {
    var pool = PoolingHttpClientConnectionManagerBuilder.create()
        .setDefaultConnectionConfig(ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(connectMillis))
            .setSocketTimeout(Timeout.ofMilliseconds(readMillis)).build());
    if (requirePublic) {
      pool.setDnsResolver(new PinnedDnsResolver(uri.getHost(), PublicUrlPolicy.resolve(uri)));
    }
    try (var client = HttpClients.custom().setConnectionManager(pool.build())
        .disableRedirectHandling().disableCookieManagement().disableAutomaticRetries().build()) {
      HttpGet request = new HttpGet(uri);
      headers.forEach(request::setHeader);
      try (var response = client.executeOpen(null, request, null)) {
        int status = response.getCode();
        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
          Header location = response.getFirstHeader(HttpHeaders.LOCATION);
          if (location != null) {
            return new Result(uri, null, null, location.getValue());
          }
        }
        if (status < 200 || status >= 300 || response.getEntity() == null) {
          throw new IOException("HTTP fetch failed with status " + status);
        }
        if (response.getEntity().getContentLength() > maxBytes) {
          throw new IOException("HTTP response exceeds the configured size limit");
        }
        byte[] body = response.getEntity().getContent().readNBytes(maxBytes + 1);
        if (body.length > maxBytes) {
          throw new IOException("HTTP response exceeds the configured size limit");
        }
        return new Result(uri, response.getEntity().getContentType(), body, null);
      }
    }
  }

  private static boolean sameOrigin(URI source, URI target) {
    return source.getScheme().equalsIgnoreCase(target.getScheme())
        && source.getHost().equalsIgnoreCase(target.getHost())
        && effectivePort(source) == effectivePort(target);
  }

  private static int effectivePort(URI uri) {
    return uri.getPort() == -1 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80) : uri.getPort();
  }

  private record PinnedDnsResolver(String host, InetAddress[] addresses) implements DnsResolver {
    @Override
    public InetAddress[] resolve(String name) throws UnknownHostException {
      resolveCanonicalHostname(name);
      return addresses.clone();
    }

    @Override
    public String resolveCanonicalHostname(String name) throws UnknownHostException {
      if (!host.equalsIgnoreCase(name) && !host.equalsIgnoreCase("[" + name + "]")) {
        throw new UnknownHostException("Unexpected HTTP destination");
      }
      return name;
    }
  }

  public record Result(URI uri, String contentType, byte[] body, String location) {}
}
