package org.freakz.common.model.feed;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@ToString
public class Message {

    private String id = "0";
    private MessageSource messageSource;
    private long timestamp;
    private long requestTimestamp;

    private LocalDateTime time;
    private String sender;
    private String target;
    private String message;

}
