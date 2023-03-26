package org.freakz.controller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.CommandHandler;
import org.freakz.services.topcounter.TopCountService;
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
    private final TopCountService countService;


    public EngineController(CommandHandler commandHandler, TopCountService countService) {
        this.commandHandler = commandHandler;
        this.countService = countService;

    }

    @PostMapping("/handle_request")
    public ResponseEntity<?> handleEngineRequest(@RequestBody EngineRequest request) {
//        log.debug("request: {}", request);
//        log.debug(">>> Start handle");
        this.countService.calculateTopCounters(request);
        this.commandHandler.handleEngineRequest(request, true);
//        log.debug("<<<< handle done");
        return ResponseEntity.ok("ack");
    }

}
