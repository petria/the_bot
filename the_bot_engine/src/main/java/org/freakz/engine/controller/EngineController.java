package org.freakz.engine.controller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.model.engine.status.StatusReportRequest;
import org.freakz.common.model.engine.status.StatusReportResponse;
import org.freakz.common.model.users.GetUsersRequest;
import org.freakz.common.model.users.GetUsersResponse;
import org.freakz.common.model.users.User;
import org.freakz.engine.commands.CommandHandler;
import org.freakz.engine.commands.util.UserAndReply;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.services.status.StatusReportService;
import org.freakz.engine.services.topcounter.TopCountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hokan/engine")
@Slf4j
public class EngineController {

    private final CommandHandler commandHandler;
    private final TopCountService countService;

    private final UsersService usersService;

    private final StatusReportService statusReportService;

    public EngineController(CommandHandler commandHandler, TopCountService countService, UsersService usersService, StatusReportService statusReportService) {
        this.commandHandler = commandHandler;
        this.countService = countService;
        this.usersService = usersService;
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

    @GetMapping("/get_users")
    public ResponseEntity<?> handleGetUsers() {
        List<User> all = (List<User>) usersService.findAll();
        GetUsersResponse response = new GetUsersResponse(all);
        return ResponseEntity.ok(response);
    }

}
