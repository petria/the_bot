package org.freakz.io.contoller;


import org.freakz.common.exception.EchoToAliasNotIrcChannelException;
import org.freakz.common.exception.InvalidEchoToAliasException;
import org.freakz.common.model.connectionmanager.SendIrcRawMessageByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.SendIrcRawMessageByEchoToAliasResponse;
import org.freakz.common.model.connectionmanager.SendMessageByEchoToAliasRequest;
import org.freakz.common.model.connectionmanager.SendMessageByEchoToAliasResponse;
import org.freakz.common.model.feed.Message;
import org.freakz.io.connections.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hokan/io/messages")
public class MessagesController {

  private static final Logger log = LoggerFactory.getLogger(MessagesController.class);

  private final ConnectionManager connectionManager;

  public MessagesController(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  @PostMapping("/send/{connectionId}")
  public ResponseEntity<?> sendMessageToConnection(
      @PathVariable int connectionId, @RequestBody Message message) {
    log.debug("to connection: {}", connectionId);
    try {
      connectionManager.sendMessageToConnection(connectionId, message);
      return ResponseEntity.ok().build();

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PostMapping("/send_raw/{connectionId}")
  public ResponseEntity<?> sendRawMessageToConnection(
      @PathVariable int connectionId, @RequestBody Message message) {
    try {
      connectionManager.sendRawMessageToConnection(connectionId, message);
      return ResponseEntity.ok().build();

    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(e.getMessage());
    }
  }

  @PostMapping("/send_message_by_echo_to_alias")
  public ResponseEntity<?> sendByEchoToAlias(@RequestBody SendMessageByEchoToAliasRequest request) {
    long startedAt = System.currentTimeMillis();
    log.debug(
        "sendByEchoToAlias start echoToAlias={} messageLength={}",
        request.getEchoToAlias(),
        request.getMessage() == null ? 0 : request.getMessage().length()
    );
    SendMessageByEchoToAliasResponse response = new SendMessageByEchoToAliasResponse();
    try {
      connectionManager.sendMessageByEchoToAlias(request.getMessage(), request.getEchoToAlias());
      response.setSentTo(request.getEchoToAlias());
      log.debug(
          "sendByEchoToAlias success echoToAlias={} durationMs={}",
          request.getEchoToAlias(),
          System.currentTimeMillis() - startedAt
      );

    } catch (InvalidEchoToAliasException e) {
      response.setSentTo("NOK:  " + e.getMessage());
      log.warn(
          "sendByEchoToAlias invalid echoToAlias={} durationMs={} error={}",
          request.getEchoToAlias(),
          System.currentTimeMillis() - startedAt,
          e.getMessage()
      );
    }
    return ResponseEntity.ok(response);
  }

  @PostMapping("/send_irc_raw_message_by_echo_to_alias")
  public ResponseEntity<?> sendIrcRawByEchoToAlias(
      @RequestBody SendIrcRawMessageByEchoToAliasRequest request) {
    log.debug("Request: {}", request);
    SendIrcRawMessageByEchoToAliasResponse response = new SendIrcRawMessageByEchoToAliasResponse();

    try {

      String serverResponse =
          connectionManager.sendIrcRawMessageByEchoToAlias(
              request.getMessage(), request.getEchoToAlias());
      response.setSentTo("OK: " + request.getEchoToAlias());
      response.setServerResponse(serverResponse);

    } catch (InvalidEchoToAliasException | EchoToAliasNotIrcChannelException e) {
      response.setSentTo("NOK:  " + e.getMessage());
    }
    return ResponseEntity.ok(response);
  }
}
