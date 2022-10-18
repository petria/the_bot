package org.freakz.clients;

import org.freakz.common.model.json.feed.Message;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "messageFeedClient", url = "localhost:8080", path = "/api/message_feed")
public interface MessageFeedClient {

    @PostMapping("/insert_batch")
    ResponseEntity<?> insertBatchToMessageFeed(@RequestBody List<Message> messages);

}
