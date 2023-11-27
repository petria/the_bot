package org.freakz.engine.commands.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.freakz.common.model.users.User;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAndReply {

    private User user;

    private String replyMessage;

}
