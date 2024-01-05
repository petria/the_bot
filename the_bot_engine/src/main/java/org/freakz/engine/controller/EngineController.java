package org.freakz.engine.controller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.engine.status.StatusReportRequest;
import org.freakz.common.model.engine.status.StatusReportResponse;
import org.freakz.engine.commands.CommandHandler;
import org.freakz.engine.commands.util.UserAndReply;
import org.freakz.engine.services.status.StatusReportService;
import org.freakz.engine.services.topcounter.TopCountService;
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

    private final StatusReportService statusReportService;

    public EngineController(CommandHandler commandHandler, TopCountService countService, StatusReportService statusReportService) {
        this.commandHandler = commandHandler;
        this.countService = countService;
        this.statusReportService = statusReportService;
    }

    @PostMapping("/handle_request")
    public ResponseEntity<?> handleEngineRequest(@RequestBody EngineRequest request) {
//        log.debug("request: {}", request);
//        log.debug(">>> Start handle");
        this.countService.calculateTopCounters(request);
        UserAndReply reply = this.commandHandler.handleEngineRequest(request, true);
        EngineResponse response
                = EngineResponse.builder()
                .message(reply.getReplyMessage())
                .user(reply.getUser())
                .build();
//        log.debug("<<<< handle done");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/handle_status_report")
    public ResponseEntity<?> handleStatusReport(@RequestBody StatusReportRequest request) {
        StatusReportResponse response = statusReportService.handleStatusReport(request);
        return ResponseEntity.ok(response);
    }

}
