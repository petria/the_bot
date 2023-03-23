package org.freakz.io.contoller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.InvalidTargetAliasException;
import org.freakz.common.model.json.connectionmanager.SendMessageByTargetAliasRequest;
import org.freakz.common.model.json.connectionmanager.SendMessageByTargetAliasResponse;
import org.freakz.common.model.json.feed.Message;
import org.freakz.io.connections.ConnectionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hokan/io/messages")
@Slf4j
public class MessagesController {

    private final ConnectionManager connectionManager;

    public MessagesController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }


    @PostMapping("/send/{connectionId}")
    public ResponseEntity<?> sendMessageToConnection(@PathVariable int connectionId, @RequestBody Message message) {
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
    public ResponseEntity<?> sendRawMessageToConnection(@PathVariable int connectionId, @RequestBody Message message) {
        try {
            connectionManager.sendRawMessageToConnection(connectionId, message);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/send_message_by_target_alias")
    public ResponseEntity<?> sendByTargetAlias(@RequestBody SendMessageByTargetAliasRequest request) {
        log.debug("Request: {}", request);
        SendMessageByTargetAliasResponse response = new SendMessageByTargetAliasResponse();
        try {
            connectionManager.sendMessageByTargetAlias(request.getMessage(), request.getTargetAlias());
            response.setSentTo(request.getTargetAlias());

        } catch (InvalidTargetAliasException e) {
            response.setSentTo("NOK:  " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

}
