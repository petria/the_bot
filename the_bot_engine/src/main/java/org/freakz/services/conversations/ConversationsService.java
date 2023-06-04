package org.freakz.services.conversations;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.CommandHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ConversationsService {

    private Map<String, Conversation> conversationMap = new HashMap<>();

    public ConversationsService() {
    }

    public void handleConversations(CommandHandler commandHandler, EngineRequest request) {
        String fromSender = request.getFromSender();
        log.debug("fromSender: {}", fromSender);
        Conversation conversation = conversationMap.get(fromSender);
        if (conversation != null) {
            processConversation(commandHandler, request, conversation);
        }
    }

    private void processConversation(CommandHandler commandHandler, EngineRequest request, Conversation conversation) {
        commandHandler.sendReplyMessage(request, "testing: " + request.getFromSender() + " -> " + conversation);
    }

    public Conversation createConversation(EngineRequest request, ConversationType type) {

        Conversation conversation = new Conversation();
        conversation.setId(System.currentTimeMillis());
        conversation.setType(type);
        conversation.setState("STARTED");

        this.conversationMap.put(request.getFromSender(), conversation);
        return conversation;
    }

}
