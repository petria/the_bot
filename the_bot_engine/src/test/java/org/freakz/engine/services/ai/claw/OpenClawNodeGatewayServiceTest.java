package org.freakz.engine.services.ai.claw;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenClawNodeGatewayServiceTest {

  private final JsonMapper objectMapper = new JsonMapper();

  @Test
  void advertisesAllSupportedNodeCommands() {
    OpenClawNodeGatewayService service =
        new OpenClawNodeGatewayService(null, objectMapper, null, null, null);
    ObjectNode params = objectMapper.createObjectNode();

    service.addNodeCommandCapabilities(params);

    List<String> commands = new ArrayList<>();
    for (JsonNode command : params.path("commands")) {
      commands.add(command.asString());
    }
    assertEquals(
        List.of(
            "hokan.send_message_by_echo_to_alias",
            "hokan.read_logs",
            "hokan.search_logs"
        ),
        commands
    );
    JsonNode permissions = params.path("permissions");
    assertTrue(permissions.path("hokan.send_message_by_echo_to_alias").asBoolean());
    assertTrue(permissions.path("hokan.read_logs").asBoolean());
    assertTrue(permissions.path("hokan.search_logs").asBoolean());
  }
}
