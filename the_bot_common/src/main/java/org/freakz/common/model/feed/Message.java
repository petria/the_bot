package org.freakz.common.model.feed;

import java.time.LocalDateTime;
import lombok.*;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@ToString
public class Message {

    private String id;
    private MessageSource messageSource;
    private long timestamp;
    private long requestTimestamp;

    private LocalDateTime time;
    private String sender;
    private String target;
    private String message;

}
