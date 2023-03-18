package org.freakz.common.model.json.feed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
