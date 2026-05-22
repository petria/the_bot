package org.freakz.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.freakz.common.model.engine.system.OpenClawSettingsResponse;
import org.freakz.common.spring.rest.RestEngineClient;
import org.freakz.web.config.TheBotWebProperties;
import org.freakz.web.system.ContainerStatus;
import org.freakz.web.system.ContainerStatusProvider;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemControllerTest {

  @Test
  void aggregatesRemoteActuatorStatus() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    expectUpActuator(server, "http://bot-io:8090", "the_bot_io", "3.0-SNAPSHOT");
    expectUpActuator(server, "http://bot-engine:8100", "the_bot_engine", "3.0-SNAPSHOT");

    SystemController.SystemStatusResponse response = controller(restTemplate).getStatus();

    assertThat(response.components()).hasSize(5);
    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-io"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("UP");
          assertThat(component.componentType()).isEqualTo("SPRING_BOOT");
          assertThat(component.artifact()).isEqualTo("the_bot_io");
          assertThat(component.version()).isEqualTo("3.0-SNAPSHOT");
          assertThat(component.uptimeSeconds()).isEqualTo(123L);
          assertThat(component.startedAt()).isNotNull();
          assertThat(component.receivedCalls()).isEqualTo(42L);
          assertThat(component.requestedCalls()).isEqualTo(17L);
          assertThat(component.containerState()).isEqualTo("running");
          assertThat(component.error()).isNull();
        });
    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-whatsapp"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("UP");
          assertThat(component.componentType()).isEqualTo("SIDECAR");
          assertThat(component.containerName()).isEqualTo("bot-whatsapp");
          assertThat(component.containerState()).isEqualTo("running");
          assertThat(component.image()).isEqualTo("image-bot-whatsapp");
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
          assertThat(component.status()).isEqualTo("DEGRADED");
          assertThat(component.error()).contains("connection refused");
        });
    server.verify();
  }

  @Test
  void mapsRestartingSidecarContainerToDegraded() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    expectUpActuator(server, "http://bot-io:8090", "the_bot_io", "3.0-SNAPSHOT");
    expectUpActuator(server, "http://bot-engine:8100", "the_bot_engine", "3.0-SNAPSHOT");

    SystemController.SystemStatusResponse response = controller(
        restTemplate,
        containerName -> {
          if ("bot-openclaw".equals(containerName)) {
            return containerStatus(containerName, "restarting");
          }
          return containerStatus(containerName, "running");
        }).getStatus();

    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-openclaw"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("DEGRADED");
          assertThat(component.containerState()).isEqualTo("restarting");
        });
    server.verify();
  }

  @Test
  void mapsExternalOpenClawHealthWithoutLocalContainer() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    expectUpActuator(server, "http://bot-io:8090", "the_bot_io", "3.0-SNAPSHOT");
    expectUpActuator(server, "http://bot-engine:8100", "the_bot_engine", "3.0-SNAPSHOT");
    server.expect(once(), requestTo("http://ubuntu-server:18889/health"))
        .andRespond(withSuccess("{\"ok\":true,\"status\":\"live\"}", MediaType.APPLICATION_JSON));

    SystemController.SystemStatusResponse response = controller(
        restTemplate,
        properties -> {
          properties.setOpenclawDeploymentMode("external");
          properties.setOpenclawGatewayWsUrl("ws://ubuntu-server:18889");
        },
        containerName -> {
          if ("bot-openclaw".equals(containerName)) {
            return ContainerStatus.missing(containerName);
          }
          return containerStatus(containerName, "running");
        }).getStatus();

    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-openclaw"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("UP");
          assertThat(component.componentType()).isEqualTo("OPENCLAW_GATEWAY");
          assertThat(component.runtimeMode()).isEqualTo("external");
          assertThat(component.baseUrl()).isEqualTo("ws://ubuntu-server:18889");
          assertThat(component.healthUrl()).isEqualTo("http://ubuntu-server:18889/health");
          assertThat(component.healthStatus()).isEqualTo("live");
          assertThat(component.containerName()).isNull();
          assertThat(component.containerError()).isNull();
          assertThat(component.error()).isNull();
        });
    server.verify();
  }

  @Test
  void showsOpenClawBackendFromBotEngineRuntimeSettings() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    expectUpActuator(server, "http://bot-io:8090", "the_bot_io", "3.0-SNAPSHOT");
    expectUpActuator(server, "http://bot-engine:8100", "the_bot_engine", "3.0-SNAPSHOT");
    server.expect(once(), requestTo("http://docker.local:18889/health"))
        .andRespond(withSuccess("{\"ok\":true,\"status\":\"live\"}", MediaType.APPLICATION_JSON));
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.getOpenClawSettings()).thenReturn(ResponseEntity.ok(new OpenClawSettingsResponse(
        "docker.local",
        "ws://docker.local:18889",
        "http://docker.local:18889",
        "http://docker.local:18889/health",
        List.of())));

    SystemController.SystemStatusResponse response = controller(
        restTemplate,
        properties -> properties.setOpenclawDeploymentMode("external"),
        containerName -> ContainerStatus.missing(containerName),
        engineClient).getStatus();

    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-openclaw"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("UP");
          assertThat(component.baseUrl()).isEqualTo("ws://docker.local:18889");
          assertThat(component.healthUrl()).isEqualTo("http://docker.local:18889/health");
        });
    server.verify();
  }

  @Test
  void mapsExternalOpenClawHealthFailureToDown() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    expectUpActuator(server, "http://bot-io:8090", "the_bot_io", "3.0-SNAPSHOT");
    expectUpActuator(server, "http://bot-engine:8100", "the_bot_engine", "3.0-SNAPSHOT");
    server.expect(once(), requestTo("http://ubuntu-server:18889/health"))
        .andRespond(withException(new IOException("connection refused")));

    SystemController.SystemStatusResponse response = controller(
        restTemplate,
        properties -> {
          properties.setOpenclawDeploymentMode("external");
          properties.setOpenclawGatewayWsUrl("ws://ubuntu-server:18889");
        },
        containerName -> ContainerStatus.missing(containerName)).getStatus();

    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-openclaw"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("DOWN");
          assertThat(component.healthUrl()).isEqualTo("http://ubuntu-server:18889/health");
          assertThat(component.error()).contains("connection refused");
        });
    server.verify();
  }

  @Test
  void mapsMissingSidecarContainerToDown() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    expectUpActuator(server, "http://bot-io:8090", "the_bot_io", "3.0-SNAPSHOT");
    expectUpActuator(server, "http://bot-engine:8100", "the_bot_engine", "3.0-SNAPSHOT");

    SystemController.SystemStatusResponse response = controller(
        restTemplate,
        containerName -> {
          if ("bot-whatsapp".equals(containerName)) {
            return ContainerStatus.missing(containerName);
          }
          return containerStatus(containerName, "running");
        }).getStatus();

    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-whatsapp"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("DOWN");
          assertThat(component.containerState()).isEqualTo("missing");
          assertThat(component.containerError()).isEqualTo("Container not found");
        });
    server.verify();
  }

  @Test
  void keepsSpringBootActuatorStatusWhenDockerStatusIsUnavailable() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    expectUpActuator(server, "http://bot-io:8090", "the_bot_io", "3.0-SNAPSHOT");
    expectUpActuator(server, "http://bot-engine:8100", "the_bot_engine", "3.0-SNAPSHOT");

    SystemController.SystemStatusResponse response = controller(
        restTemplate,
        containerName -> ContainerStatus.error(containerName, "Permission denied")).getStatus();

    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-io"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("UP");
          assertThat(component.containerState()).isEqualTo("unknown");
          assertThat(component.containerError()).isEqualTo("Permission denied");
        });
    assertThat(response.components())
        .filteredOn(component -> component.name().equals("bot-whatsapp"))
        .singleElement()
        .satisfies(component -> {
          assertThat(component.status()).isEqualTo("UNKNOWN");
          assertThat(component.containerError()).isEqualTo("Permission denied");
        });
    server.verify();
  }

  private SystemController controller(RestTemplate restTemplate) {
    return controller(restTemplate, containerName -> containerStatus(containerName, "running"));
  }

  private SystemController controller(RestTemplate restTemplate, ContainerStatusProvider containerStatusProvider) {
    return controller(restTemplate, properties -> {
    }, containerStatusProvider);
  }

  private SystemController controller(
      RestTemplate restTemplate,
      Consumer<TheBotWebProperties> propertiesCustomizer,
      ContainerStatusProvider containerStatusProvider) {
    TheBotWebProperties properties = new TheBotWebProperties();
    properties.setBotIoBaseUrl("http://bot-io:8090");
    properties.setBotEngineBaseUrl("http://bot-engine:8100");
    properties.setDockerStatusEnabled(true);
    properties.setOpenclawDeploymentMode("local");
    propertiesCustomizer.accept(properties);
    RestEngineClient engineClient = mock(RestEngineClient.class);
    when(engineClient.getOpenClawSettings()).thenReturn(ResponseEntity.ok(new OpenClawSettingsResponse(
        null,
        properties.getOpenclawGatewayWsUrl(),
        null,
        null,
        List.of())));
    return controller(restTemplate, propertiesCustomizer, containerStatusProvider, engineClient);
  }

  private SystemController controller(
      RestTemplate restTemplate,
      Consumer<TheBotWebProperties> propertiesCustomizer,
      ContainerStatusProvider containerStatusProvider,
      RestEngineClient engineClient) {
    TheBotWebProperties properties = new TheBotWebProperties();
    properties.setBotIoBaseUrl("http://bot-io:8090");
    properties.setBotEngineBaseUrl("http://bot-engine:8100");
    properties.setDockerStatusEnabled(true);
    properties.setOpenclawDeploymentMode("local");
    propertiesCustomizer.accept(properties);
    return new SystemController(
        restTemplate,
        properties,
        new StandardEnvironment(),
        Optional.empty(),
        new SimpleMeterRegistry(),
        containerStatusProvider,
        engineClient);
  }

  private ContainerStatus containerStatus(String containerName, String state) {
    return new ContainerStatus(
        containerName,
        state,
        state + " for test",
        "image-" + containerName,
        Instant.parse("2026-05-01T10:00:00Z"),
        1L,
        null);
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
