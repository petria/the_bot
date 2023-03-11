package org.freakz.io.contoller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.feed.Message;
import org.freakz.io.connections.BotConnection;
import org.freakz.io.connections.ConnectionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/hokan/io/messages")
@Slf4j
public class MessagesController {

    private final ConnectionManager connectionManager;

    public MessagesController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @GetMapping("/connection_map")
    public ResponseEntity<?> getConnectionMap() {
        Map<Integer, BotConnection> connectionMap = connectionManager.getConnectionMap();
        return ResponseEntity.ok(connectionMap);
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


}
