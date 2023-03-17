package org.freakz.io.contoller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.dto.BotConnection;
import org.freakz.common.model.json.connectionmanager.GetConnectionMapResponse;
import org.freakz.io.connections.ConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/hokan/io/connection_manager")
@Slf4j
public class ConnectionManagerController {

    private final ConnectionManager connectionManager;

    @Autowired
    public ConnectionManagerController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @GetMapping("/get_connection_map")
    public ResponseEntity<?> getConnectionMap() {
        Map<Integer, BotConnection> connectionMap = connectionManager.getConnectionMap();
        GetConnectionMapResponse response = new GetConnectionMapResponse();
        response.setConnectionMap(connectionMap);
        return ResponseEntity.ok(response);
    }

}
