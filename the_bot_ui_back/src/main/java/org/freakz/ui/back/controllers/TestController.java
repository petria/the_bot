package org.freakz.ui.back.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.engine.EngineResponse;
import org.freakz.common.util.FeignUtils;
import org.freakz.ui.back.clients.EngineClient;
import org.freakz.ui.back.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/test")
@Slf4j
public class TestController {

  @Autowired
  EngineClient engineClient;

  @GetMapping("/all")
  public String allAccess() {
    return "Public Content.";
  }

  @GetMapping("/user")
  @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
  public String userAccess() {
    return "User Content.";
  }

  @GetMapping("/mod")
  @PreAuthorize("hasRole('MODERATOR')")
  public String moderatorAccess() {
    return "Moderator Board.";
  }

  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public String adminAccess() {
    UserDetailsImpl details = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String test = sendToServer("!cmpweather jaipur oulu helsinki", details.getUsername());
    return "Admin Board\n\n" + test;
  }

  @GetMapping("/cmd/{command}")
  @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
  public String handleCommand(@PathVariable String command){
    UserDetailsImpl details = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String test = sendToServer(command, details.getUsername());
    return test;
  }

  public String sendToServer(String message, String botUser) {


    EngineRequest request
            = EngineRequest.builder()
            .fromChannelId(-1L)
            .timestamp(System.currentTimeMillis())
            .command(message)
            .replyTo("NO_REPLY")
            .fromConnectionId(-1)
            .fromSender(botUser)
            .fromSenderId("NO_SENDER_ID")
            .network("BOT_WEB_CLIENT")
            .build();
    try {
      Response response = engineClient.handleEngineRequest(request);
      if (response.status() != 200) {
        log.error("{}: Engine not running: {}", response.status(), response.reason());
      } else {
        Optional<EngineResponse> responseBody = FeignUtils.getResponseBody(response, EngineResponse.class, new ObjectMapper());
        if (responseBody.isPresent()) {
          EngineResponse engineResponse = responseBody.get();
//                    log.debug("EngineResponse: {}", engineResponse);
          return engineResponse.getMessage();
        } else {
          log.error("No EngineResponse!?");
        }
      }
    } catch (Exception e) {
      log.error("Unable to send to Engine: {}", e.getMessage());
    }

    return "<error>";
  }
}
