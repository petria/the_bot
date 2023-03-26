package org.freakz.common.model.json.engine;

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
public class EngineRequest {

    private long timestamp;
    private String command;
    private String replyTo;
    private int fromConnectionId;

    private Long fromChannelId;

    private String fromSender;

    private String network;

    public String getMessage() {
        return command;
    }
}
