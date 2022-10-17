package org.freakz.common.model.json.feed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@ToString
public class Message {

    private long id = 0;
    private long timestamp;
    private String sender;
    private String message;



}
