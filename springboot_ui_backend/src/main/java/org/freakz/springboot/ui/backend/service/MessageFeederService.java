package org.freakz.springboot.ui.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.feed.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageFeederService {

    private final List<Message> feed = new ArrayList<>();

    public MessageFeederService() {
    }

    //    @PostConstruct
    public void initFeed() {
        int counter = 1;
        long timestamp = System.currentTimeMillis();
        Message msg = Message.builder()
                .timestamp(timestamp)
                .sender("sender: " + counter)
                .message("msg: " + counter)
                .build();
//        feed.put(msg.getTimestamp(), msg);

        counter++;

        msg = Message.builder()
                .timestamp((timestamp - (1000 * 5)))
                .sender("sender: " + counter)
                .message("msg: " + counter)
                .build();
//        feed.put(msg.getTimestamp(), msg);

        counter++;

        msg = Message.builder()
                .timestamp((timestamp - (1000 * 10)))
                .sender("sender: " + counter)
                .message("msg: " + counter)
                .build();
        //      feed.put(msg.getTimestamp(), msg);

        counter++;

    }


    public List<Message> getMessagesSinceTimestamp(long timestamp) {
        List<Message> collect = this.feed.stream().filter(m -> m.getTimestamp() > timestamp).collect(Collectors.toList());
        return collect;
    }

    public int insertMessage(Message message) {
        message.setTimestamp(System.currentTimeMillis());
        message.setId(this.feed.size());
        this.feed.add(0, message);
        return this.feed.size();
    }

    public List<Message> getLastMessages(long max) {
        List<Message> list = new ArrayList<>();
        int count = 0;
        for (Message message : this.feed) {
            list.add(message);
            count++;
            if (count == max) {
                break;
            }
        }
        return list;
    }

    public List<Message> getMessagesAfterId(long id) {
        List<Message> list = new ArrayList<>();
        for (Message message : this.feed) {
            if (message.getId() > id) {
                list.add(message);
            }
        }
        return list;
    }
}
