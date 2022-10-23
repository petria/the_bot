package org.freakz.io.contoller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.InvalidChannelIdException;
import org.freakz.common.model.json.feed.Message;
import org.freakz.connections.BotConnection;
import org.freakz.connections.ConnectionManager;
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
    public ResponseEntity<?> sendMessageToConnection(@PathVariable int connectionId, @RequestBody Message message) throws InvalidChannelIdException {
        connectionManager.sendMessageToConnection(connectionId, message);
        return ResponseEntity.ok().build();
    }
}
