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

import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/message_feed")
@Slf4j
public class MessageFeedController {

    @Autowired
    private MessageFeederService messageFeeder;

    private static int count = 0;
    @GetMapping("/since/{timestamp}")
    public ResponseEntity<?> getMessagesSinceTimestamp(@PathVariable("timestamp") long timestamp) {
        List<Message> list = new ArrayList<>();
        int rnd = 1 + (int) (Math.random() * 100);
        if (rnd > 50) {
            Message message = Message.builder()
                    .timestamp(System.currentTimeMillis())
                    .sender("Sender " + count)
                    .message("Message " + count)
                    .build();
            count++;
            list.add(message);
        }



//        List<Message> messages = messageFeeder.getMessagesSinceTimestamp(0);
        return ResponseEntity.ok(list);
    }

}
