package org.freakz.springboot.ui.backend.controllers;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.feed.Message;
import org.freakz.springboot.ui.backend.service.MessageFeederService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/message_feed")
@Slf4j
public class MessageFeedController {

    @Autowired
    private MessageFeederService messageFeeder;

    @GetMapping("/since/{timestamp}")
    public ResponseEntity<?> getMessagesSinceTimestamp(@PathVariable("timestamp") long timestamp) {
        List<Message> messages = messageFeeder.getMessagesSinceTimestamp(0);
        return ResponseEntity.ok(messages);
    }

}
