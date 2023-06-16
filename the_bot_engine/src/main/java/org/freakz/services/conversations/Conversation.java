package org.freakz.services.conversations;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.freakz.common.model.engine.EngineRequest;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class Conversation {

    private long id;

    private ConversationType type;

    private int state = 0;

    private String trigger;

    private ConversationContent content;

    public int nextState() {
        this.state++;
        return this.state;
    }

    public String handleConversation(EngineRequest request) {
        return this.content.handleConversation(request, state);
    }
}
