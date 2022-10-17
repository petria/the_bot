package org.freakz.springboot.ui.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.feed.Message;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageFeederService {

    private final Map<Long, Message> feed = new TreeMap<>();

    @PostConstruct
    public void initFeed() {
        int counter = 1;
        long timestamp = System.currentTimeMillis();
        Message msg = Message.builder()
                .timestamp(timestamp)
                .sender("sender: " + counter)
                .message("msg: " + counter)
                .build();
        feed.put(msg.getTimestamp(), msg);

        counter++;

        msg = Message.builder()
                .timestamp((timestamp - (1000 * 5)))
                .sender("sender: " + counter)
                .message("msg: " + counter)
                .build();
        feed.put(msg.getTimestamp(), msg);

        counter++;

        msg = Message.builder()
                .timestamp((timestamp - (1000 * 10)))
                .sender("sender: " + counter)
                .message("msg: " + counter)
                .build();
        feed.put(msg.getTimestamp(), msg);

        counter++;

    }


    public List<Message> getMessagesSinceTimestamp(long timestamp) {
        List<Message> collect = this.feed.values().stream().filter(m -> m.getTimestamp() > timestamp).collect(Collectors.toList());
        return collect;
    }

}
