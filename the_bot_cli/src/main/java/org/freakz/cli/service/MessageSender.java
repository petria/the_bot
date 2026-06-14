package org.freakz.cli.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class MessageSender {

  private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

  private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
  private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
  private final HttpClient httpClient = HttpClient.newBuilder()
      .cookieHandler(cookieManager)
      .connectTimeout(Duration.ofSeconds(5))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  private String baseUrl;
  private String loggedInUsername;

  public LoginSession login(String target, String username, String password) {
    List<String> candidates = resolveCandidates(target);
    CliClientException lastNetworkFailure = null;
    for (String candidate : candidates) {
      try {
        LoginSession session = tryLogin(candidate, username, password);
        this.baseUrl = candidate;
        this.loggedInUsername = session.username();
        return session;
      } catch (InvalidCredentialsException e) {
        throw e;
      } catch (CliClientException e) {
        lastNetworkFailure = e;
      }
    }
    if (lastNetworkFailure != null) {
      throw lastNetworkFailure;
    }
    throw new CliClientException("Could not connect to target host");
  }

  public String sendToServer(String message) {
    ensureLoggedIn();
    if (message == null || message.isBlank()) {
      throw new CliClientException("Command is required");
    }
    HttpRequest request = jsonPost("/api/web/cli/command", """
        {"command":%s}
        """.formatted(jsonString(message.trim())));
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 401 || response.statusCode() == 403) {
        throw new CliClientException("CLI session is no longer authenticated");
      }
      if (response.statusCode() != 200) {
        throw new CliClientException("Command failed with HTTP " + response.statusCode());
      }
      JsonNode body = jsonMapper.readTree(response.body());
      return body.path("reply").asText("");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CliClientException("Could not send command: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new CliClientException("Could not send command: " + e.getMessage(), e);
    }
  }

  public void logout() {
    if (baseUrl == null) {
      return;
    }
    try {
      HttpResponse<String> response = httpClient.send(
          jsonPost("/api/web/cli/logout", "{}"),
          HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400 && response.statusCode() != 401 && response.statusCode() != 403) {
        log.warn("CLI logout returned HTTP {}", response.statusCode());
      }
    } catch (Exception e) {
      log.warn("CLI logout failed: {}", e.getMessage());
    } finally {
      cookieManager.getCookieStore().removeAll();
      baseUrl = null;
      loggedInUsername = null;
    }
  }

  public String loggedInUsername() {
    return loggedInUsername;
  }

  public String baseUrl() {
    return baseUrl;
  }

  static List<String> resolveCandidates(String target) {
    if (target == null || target.isBlank()) {
      throw new IllegalArgumentException("Target host is required");
    }
    String normalized = target.trim().replaceFirst("/+$", "");
    if (normalized.contains("://")) {
      return List.of(normalized);
    }
    List<String> candidates = new ArrayList<>();
    candidates.add("https://" + normalized);
    if (normalized.contains(":")) {
      candidates.add("http://" + normalized);
    } else {
      candidates.add("http://" + normalized + ":8091");
    }
    return candidates;
  }

  private LoginSession tryLogin(String candidateBaseUrl, String username, String password) {
    cookieManager.getCookieStore().removeAll();
    HttpRequest request = HttpRequest.newBuilder(uri(candidateBaseUrl + "/api/web/cli/login"))
        .timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("""
            {"username":%s,"password":%s}
            """.formatted(jsonString(username), jsonString(password))))
        .build();
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 401) {
        throw new InvalidCredentialsException("Invalid username or password");
      }
      if (response.statusCode() == 404) {
        throw new CliClientException("CLI API not found at " + candidateBaseUrl);
      }
      if (response.statusCode() != 200) {
        throw new CliClientException("Login failed with HTTP " + response.statusCode());
      }
      JsonNode body = jsonMapper.readTree(response.body());
      return new LoginSession(
          body.path("username").asText(username),
          body.path("name").asText(null),
          candidateBaseUrl);
    } catch (InvalidCredentialsException e) {
      throw e;
    } catch (IOException e) {
      throw new CliClientException("Could not connect to " + candidateBaseUrl + ": " + e.getMessage(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CliClientException("Login interrupted", e);
    }
  }

  private HttpRequest jsonPost(String path, String body) {
    ensureLoggedIn();
    return HttpRequest.newBuilder(uri(baseUrl + path))
        .timeout(Duration.ofSeconds(30))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
  }

  private URI uri(String value) {
    try {
      return new URI(value);
    } catch (URISyntaxException e) {
      throw new CliClientException("Invalid target URL: " + value, e);
    }
  }

  private String jsonString(String value) {
    return jsonMapper.writeValueAsString(value);
  }

  private void ensureLoggedIn() {
    if (baseUrl == null || loggedInUsername == null) {
      throw new CliClientException("CLI is not logged in");
    }
  }

  public record LoginSession(String username, String name, String baseUrl) {
  }

  public static class CliClientException extends RuntimeException {
    public CliClientException(String message) {
      super(message);
    }

    public CliClientException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class InvalidCredentialsException extends CliClientException {
    public InvalidCredentialsException(String message) {
      super(message);
    }
  }
}
