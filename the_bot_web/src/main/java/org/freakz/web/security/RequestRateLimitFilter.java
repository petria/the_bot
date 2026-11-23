package org.freakz.web.security;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

final class RequestRateLimitFilter extends OncePerRequestFilter {

  static final String CLIENT_ADDRESS = RequestRateLimitFilter.class.getName() + ".clientAddress";
  private static final int MAX_AUTH_BODY = 16 * 1024;
  private static final int AUTH_WINDOW = 15 * 60;

  private final Set<String> trustedProxies;
  private final WebLoginFailureHandler alerts;
  private final JsonMapper mapper;
  private final Clock clock;
  private final Budget media = new Budget(30, 60);
  private final Budget addresses = new Budget(60, AUTH_WINDOW);
  private final Budget accountFailures = new Budget(5, AUTH_WINDOW);
  private final PathPatternRequestMatcher mediaPath = PathPatternRequestMatcher.pathPattern("/m/**");
  private final PathPatternRequestMatcher formLogin = PathPatternRequestMatcher.pathPattern("/login");
  private final PathPatternRequestMatcher cliLogin = PathPatternRequestMatcher.pathPattern("/api/web/cli/login");
  private final PathPatternRequestMatcher mobileLogin = PathPatternRequestMatcher.pathPattern("/api/mobile/auth/login");
  private final PathPatternRequestMatcher refresh = PathPatternRequestMatcher.pathPattern("/api/mobile/auth/refresh");

  RequestRateLimitFilter(Set<String> trustedProxies, WebLoginFailureHandler alerts, JsonMapper mapper, Clock clock) {
    this.trustedProxies = trustedProxies;
    this.alerts = alerts;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long now = clock.instant().getEpochSecond();
    if (mediaPath.matches(request)) {
      if (available(media.take(clientAddress(request), now), now, response)) {
        chain.doFilter(request, response);
      }
      return;
    }
    boolean form = formLogin.matches(request);
    boolean login = cliLogin.matches(request) || mobileLogin.matches(request);
    if (!"POST".equals(request.getMethod()) || !(form || login || refresh.matches(request))) {
      chain.doFilter(request, response);
      return;
    }

    String address = clientAddress(request);
    request.setAttribute(CLIENT_ADDRESS, address);
    if (!available(addresses.take(address, now), now, response)) {
      return;
    }

    String username = null;
    if (form) {
      username = request.getParameter("username");
    } else if (login) {
      byte[] body = request.getInputStream().readNBytes(MAX_AUTH_BODY + 1);
      if (body.length > MAX_AUTH_BODY) {
        response.setStatus(413);
        return;
      }
      request = new ReplayableRequest(request, body);
      try {
        var json = mapper.readTree(body);
        username = json == null ? null : json.path("username").asText(null);
      } catch (JacksonException ignored) {
        // MVC returns the validation response for malformed bodies.
      }
    }
    String key = address + "\n" + normalizeAccount(username);
    Ticket ticket = form || login ? accountFailures.take(key, now) : null;
    if (ticket != null && !available(ticket, now, response)) {
      return;
    }

    try {
      chain.doFilter(request, response);
    } finally {
      String location = response.getHeader(HttpHeaders.LOCATION);
      boolean rejected = form ? location != null && location.endsWith("/login?error") : response.getStatus() == 401;
      if (ticket != null && !rejected) {
        accountFailures.release(key, ticket);
      } else if (login && rejected) {
        alerts.reportFailedLogin(request, username);
      }
    }
  }

  private boolean available(Ticket ticket, long now, HttpServletResponse response) {
    if (ticket.allowed()) return true;
    response.setStatus(429);
    response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(1, ticket.window().expires - now)));
    return false;
  }

  private String clientAddress(HttpServletRequest request) {
    String remote = request.getRemoteAddr();
    if (trustedProxies.contains(remote)) {
      String supplied = request.getHeader("X-Real-IP");
      if (supplied != null) {
        try {
          return InetAddress.ofLiteral(supplied.trim()).getHostAddress();
        } catch (IllegalArgumentException ignored) {
          // Only an IP literal supplied by the trusted proxy is accepted.
        }
      }
    }
    return remote;
  }

  private String normalizeAccount(String username) {
    if (username == null) return "";
    String trimmed = username.strip().toLowerCase(Locale.ROOT);
    return trimmed.substring(0, Math.min(128, trimmed.length()));
  }

  private static final class Budget {
    private final int limit;
    private final int seconds;
    private final Map<String, Window> windows = new HashMap<>();
    private long nextCleanup;

    private Budget(int limit, int seconds) {
      this.limit = limit;
      this.seconds = seconds;
    }

    synchronized Ticket take(String key, long now) {
      if (now >= nextCleanup) {
        windows.values().removeIf(window -> window.expires <= now);
        nextCleanup = now + 60;
      }
      Window window = windows.get(key);
      if (window == null || window.expires <= now) {
        if (window == null && windows.size() >= 20000) {
          return new Ticket(false, new Window(nextCleanup));
        }
        window = new Window(now + seconds);
        windows.put(key, window);
      }
      if (window.count >= limit) return new Ticket(false, window);
      window.count++;
      return new Ticket(true, window);
    }

    synchronized void release(String key, Ticket ticket) {
      if (ticket.window() == windows.get(key) && --ticket.window().count == 0) {
        windows.remove(key);
      }
    }
  }

  private static final class Window {
    private final long expires;
    private int count;

    private Window(long expires) {
      this.expires = expires;
    }
  }

  private record Ticket(boolean allowed, Window window) {}

  private static final class ReplayableRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    ReplayableRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body;
    }

    @Override
    public ServletInputStream getInputStream() {
      return new ServletInputStream() {
        private final ByteArrayInputStream input = new ByteArrayInputStream(body);

        @Override public boolean isFinished() { return input.available() == 0; }
        @Override public boolean isReady() { return true; }
        @Override public int read() { return input.read(); }
        @Override public int read(byte[] buffer, int offset, int length) { return input.read(buffer, offset, length); }

        @Override
        public void setReadListener(ReadListener listener) {
          throw new IllegalStateException("Authentication requests use synchronous body reads");
        }
      };
    }

    @Override
    public BufferedReader getReader() {
      return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
  }
}
