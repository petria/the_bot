package org.freakz.services.conversations;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Conversation {

    private long id;

    private ConversationType type;

    private String state;


}
