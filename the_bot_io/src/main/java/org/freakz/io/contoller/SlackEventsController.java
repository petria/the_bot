package org.freakz.io.contoller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.slack.SlackEvent;
import org.freakz.common.model.slack.UrlVerificationRequest;
import org.freakz.common.model.slack.UrlVerificationResponse;
import org.freakz.io.connections.SlackConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hokan/io/slack")
@Slf4j
public class SlackEventsController {


    private SlackConnection slackConnection;

//    public SlackEventsController(ConnectionManager connectionManager) {
//        this.connectionManager = connectionManager;
//    }
// https://ebcd-2001-14ba-7241-df00-4f4-59f9-2bf1-fde2.ngrok-free.app/api/hokan/io/slack
// https://xububuntu.airiot.fi:8090/api/hokan/io/slack
//    @PostMapping
public ResponseEntity<?> urlVerification(@RequestBody UrlVerificationRequest request) {
    UrlVerificationResponse response = new UrlVerificationResponse();
    response.setChallenge(request.getChallenge());
    return ResponseEntity.ok(response);
}

    /*
    https://api.slack.com/apps/A07JGLWG8BS/event-subscriptions?
     */
    @PostMapping
    public ResponseEntity<?> handleSlackEvent(@RequestBody SlackEvent event) {


//        log.debug("Slack event: {}", event);
        if (this.slackConnection != null) {
            slackConnection.handleSlackEvent(event);
        }

        return ResponseEntity.ok().build();
    }

    public void init(SlackConnection slackConnection) {
        this.slackConnection = slackConnection;
    }
}
