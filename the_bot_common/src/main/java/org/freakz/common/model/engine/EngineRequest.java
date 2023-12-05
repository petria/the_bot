package org.freakz.common.model.engine;

import lombok.*;
import org.freakz.common.model.users.User;

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

    private boolean isPrivateChannel;
    private Long fromChannelId;

    private String fromSenderId;
    private String fromSender;
    private boolean isFromAdmin = false;

    private String network;

    private User user;

    public String getMessage() {
        return command;
    }

    public boolean isPrivateChannel() {
        return false;
    }
}
