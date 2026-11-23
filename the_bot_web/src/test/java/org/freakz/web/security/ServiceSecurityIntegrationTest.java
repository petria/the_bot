package org.freakz.web.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.freakz.web.mobile.MobileBearerAuthenticationFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = ServiceSecurityIntegrationTest.Fixture.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"server.servlet.context-path=/fixture", "the.bot.internal-api-token=test-token"})
class ServiceSecurityIntegrationTest {

  @LocalServerPort
  private int port;

  @MockitoBean
  private WebLoginFailureHandler failureHandler;

  @MockitoBean
  private MobileBearerAuthenticationFilter mobileFilter;

  @Test
  void requiresServiceCredentialsAndAllowsStatelessWritesUnderAContextPath() throws Exception {
    URI uri = URI.create("http://localhost:" + port + "/fixture/internal/test");
    try (HttpClient client = HttpClient.newHttpClient()) {
      var denied = client.send(HttpRequest.newBuilder(uri)
          .header("Cookie", "JSESSIONID=untrusted")
          .POST(HttpRequest.BodyPublishers.noBody())
          .build(), HttpResponse.BodyHandlers.ofString());
      assertThat(denied.statusCode()).isEqualTo(401);

      var accepted = client.send(HttpRequest.newBuilder(uri)
          .header("X-TheBot-Internal-Token", "test-token")
          .POST(HttpRequest.BodyPublishers.noBody())
          .build(), HttpResponse.BodyHandlers.ofString());
      assertThat(accepted.statusCode()).isEqualTo(200);
      assertThat(accepted.body()).isEqualTo("bot-internal-service");
      assertThat(accepted.headers().allValues("Set-Cookie")).isEmpty();
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @Import({SecurityConfig.class, FixtureController.class})
  static class Fixture {}

  @RestController
  static class FixtureController {
    @PostMapping("/internal/test")
    String service(Principal principal) {
      return principal.getName();
    }
  }
}
