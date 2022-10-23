package org.freakz.springboot.ui.backend.controllers;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.feed.Message;
import org.freakz.common.payload.response.MessageFeedResponse;
import org.freakz.springboot.ui.backend.clients.BotIOClient;
import org.freakz.springboot.ui.backend.service.MessageFeederService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/message_feed")
@Slf4j
public class MessageFeedController {

    @Autowired
    private MessageFeederService messageFeeder;

    @Autowired
    private BotIOClient botIOClient;

    @GetMapping("/current_day")
    public ResponseEntity<?> getMessagesOfCurrentDay() {
        List<Message> list = messageFeeder.getMessagesForDay(LocalDate.now());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/after_id/{id}")
    public ResponseEntity<?> getMessagesAfterId(@PathVariable("id") long id) {
        Response response = botIOClient.getMessagesAfterId(id);
        Optional<MessageFeedResponse> responseBody = FeignUtils.getResponseBody(response, MessageFeedResponse.class);

//        List<Message> list = messageFeeder.getMessagesAfterId(id);
//        log.debug("after id {} -> {}", id, list.size());
        return ResponseEntity.ok(responseBody.get().getMessages());
    }

    @GetMapping("/last/{max}")
    public ResponseEntity<?> getMessagesLastMessages(@PathVariable("max") long max) {
        List<Message> list = messageFeeder.getLastMessages(max);
        log.debug("last {} -> {}", max, list.size());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/since/{timestamp}")
    public ResponseEntity<?> getMessagesSinceTimestamp(@PathVariable("timestamp") long timestamp) {
        List<Message> list = messageFeeder.getMessagesSinceTimestamp(timestamp);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insertToMessageFeed(@RequestBody Message message) {
        log.debug("Insert to feed: {}", message);
        int count = messageFeeder.insertMessage(message);
        log.debug("Feed size now: {}", count);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/insert_batch")
    public ResponseEntity<?> insertBatchToMessageFeed(@RequestBody List<Message> messages) {

        log.debug("Insert batch to feed: {}", messages.size());

        for (Message message : messages) {
            messageFeeder.insertMessage(message);
        }

        log.debug("Feed size now: {}", messageFeeder.getCount());
        return ResponseEntity.ok().build();
    }

}
