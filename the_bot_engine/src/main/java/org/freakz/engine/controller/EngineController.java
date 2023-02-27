package org.freakz.engine.controller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.CommandHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hokan/engine")
@Slf4j
public class EngineController {

    private final CommandHandler commandHandler;

    public EngineController(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping("/handle_request")
    public ResponseEntity<?> handleEngineRequest(@RequestBody EngineRequest request) {
        log.debug("request: {}", request);
/*        String reply = "";
        if (request.getCommand().equals("!ping")) {
            reply = "Pong: " + System.currentTimeMillis();
        }*/
        log.debug(">>> Start handle");
        this.commandHandler.handleCommand(request);
        log.debug("<<<< handle done");
        return ResponseEntity.ok("ack");
    }

}
