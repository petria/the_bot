package org.freakz.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.Optional;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.freakz.web.config.TheBotWebProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class SystemControllerTest {

  @Test
  void aggregatesRemoteActuatorStatus() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    expectUpActuator(server, "http://bot-io:8090", "the_bot_io", "3.0-SNAPSHOT");
    expectUpActuator(server, "http://bot-engine:8100", "the_bot_engine", "3.0-SNAPSHOT");

    SystemController.SystemStatusResponse response = controller(restTemplate).getStatus();

    assertThat(response.components()).hasSize(3);
    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-io"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("UP");
          assertThat(component.artifact()).isEqualTo("the_bot_io");
          assertThat(component.version()).isEqualTo("3.0-SNAPSHOT");
          assertThat(component.uptimeSeconds()).isEqualTo(123L);
          assertThat(component.startedAt()).isNotNull();
          assertThat(component.receivedCalls()).isEqualTo(42L);
          assertThat(component.requestedCalls()).isEqualTo(17L);
          assertThat(component.error()).isNull();
        });
    server.verify();
  }

  @Test
  void mapsUnreachableRemoteActuatorStatusToDown() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    server.expect(once(), requestTo("http://bot-io:8090/actuator/health"))
        .andRespond(withException(new IOException("connection refused")));
    expectUpActuator(server, "http://bot-engine:8100", "the_bot_engine", "3.0-SNAPSHOT");

    SystemController.SystemStatusResponse response = controller(restTemplate).getStatus();

    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-io"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("DOWN");
          assertThat(component.error()).contains("connection refused");
        });
    server.verify();
  }

  private SystemController controller(RestTemplate restTemplate) {
    TheBotWebProperties properties = new TheBotWebProperties();
    properties.setBotIoBaseUrl("http://bot-io:8090");
    properties.setBotEngineBaseUrl("http://bot-engine:8100");
    return new SystemController(restTemplate, properties, new StandardEnvironment(), Optional.empty(), new SimpleMeterRegistry());
  }

  private void expectUpActuator(
      MockRestServiceServer server,
      String baseUrl,
      String artifact,
      String version) {
    server.expect(once(), requestTo(baseUrl + "/actuator/health"))
        .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));
    server.expect(once(), requestTo(baseUrl + "/actuator/info"))
        .andRespond(withSuccess(
            "{\"build\":{\"artifact\":\"" + artifact + "\",\"version\":\"" + version + "\"}}",
            MediaType.APPLICATION_JSON));
    server.expect(once(), requestTo(baseUrl + "/actuator/metrics/process.uptime"))
        .andRespond(withSuccess(
            "{\"measurements\":[{\"statistic\":\"VALUE\",\"value\":123.4}]}",
            MediaType.APPLICATION_JSON));
    server.expect(once(), requestTo(baseUrl + "/actuator/metrics/process.start.time"))
        .andRespond(withSuccess(
            "{\"measurements\":[{\"statistic\":\"VALUE\",\"value\":1778070000.0}]}",
            MediaType.APPLICATION_JSON));
    server.expect(once(), requestTo(baseUrl + "/actuator/metrics/http.server.requests"))
        .andRespond(withSuccess(
            "{\"measurements\":[{\"statistic\":\"COUNT\",\"value\":42.0}]}",
            MediaType.APPLICATION_JSON));
    server.expect(once(), requestTo(baseUrl + "/actuator/metrics/thebot.http.client.requests"))
        .andRespond(withSuccess(
            "{\"measurements\":[{\"statistic\":\"COUNT\",\"value\":17.0}]}",
            MediaType.APPLICATION_JSON));
  }
}
