package org.freakz.io.contoller;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.feed.Message;
import org.freakz.common.payload.response.MessageFeedResponse;
import org.freakz.io.service.MessageFeederService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hokan/io/message_feed")
@Slf4j
public class MessageFeedController {

    @Autowired
    private MessageFeederService messageFeeder;

    @GetMapping("/after_id/{id}")
    public ResponseEntity<?> getMessagesAfterId(@PathVariable("id") long id) {
        List<Message> list = messageFeeder.getMessagesAfterId(id);
        log.debug("after id {} -> {}", id, list.size());
        MessageFeedResponse response
                = MessageFeedResponse.builder()
                .messages(list)
                .build();
        return ResponseEntity.ok(response);
    }

}
