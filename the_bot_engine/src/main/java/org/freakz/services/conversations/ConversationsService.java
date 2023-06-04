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

    private static long nextId = 0;
    private Map<String, Conversation> conversationMap = new HashMap<>();

    public ConversationsService() {
    }

    private long nextId() {
        nextId++;
        return nextId;
    }

    public void handleConversations(CommandHandler commandHandler, EngineRequest request) {
        String fromSender = request.getFromSender();
        Conversation conversation = conversationMap.get(fromSender);
        if (conversation != null) {
            log.debug("fromSender: {}", fromSender);
            processConversation(commandHandler, request, conversation);
        }
    }

    private void processConversation(CommandHandler commandHandler, EngineRequest request, Conversation conversation) {
        if (request.getMessage().matches(conversation.getTrigger())) {
            commandHandler.sendReplyMessage(request, conversation.handleConversation(request));
            conversation.nextState();
        }
    }

    public Conversation createConversation(EngineRequest request, ConversationType type) {

        if (this.conversationMap.containsKey(request.getFromSender())) {
            return null;
        }

        Conversation conversation = new Conversation();
        conversation.setId(nextId());
        conversation.setType(type);
        conversation.setTrigger("cv:.*");

        ConversationContent content;

        switch (type) {
            default:
            case QUIZ:
                Quiz quiz = new Quiz("Capitol quiz");

                Quiz.QuizQuestion question
                        = Quiz.QuizQuestion.builder()
                        .question("Turku")
                        .build();

                Quiz.QuizStep step
                        = Quiz.QuizStep.builder()
                        .step("What is the capitol of Finland")
                        .questionMap(new HashMap<>())
                        .build();

                step.getQuestionMap().put("1", Quiz.QuizQuestion.builder().question("Turku").isCorrect(false).build());
                step.getQuestionMap().put("2", Quiz.QuizQuestion.builder().question("Hyvinkää").isCorrect(false).build());
                step.getQuestionMap().put("3", Quiz.QuizQuestion.builder().question("Helsinki").isCorrect(true).build());
                step.getQuestionMap().put("4", Quiz.QuizQuestion.builder().question("Oulu").isCorrect(false).build());

                quiz.getStepMap().put("0", step);

                step
                        = Quiz.QuizStep.builder()
                        .step("What is Ceki's dog name?")
                        .questionMap(new HashMap<>())
                        .build();

                step.getQuestionMap().put("1", Quiz.QuizQuestion.builder().question("Wheeny").isCorrect(false).build());
                step.getQuestionMap().put("2", Quiz.QuizQuestion.builder().question("Pheeny").isCorrect(false).build());
                step.getQuestionMap().put("3", Quiz.QuizQuestion.builder().question("Eeny").isCorrect(true).build());
                step.getQuestionMap().put("4", Quiz.QuizQuestion.builder().question("Dont know").isCorrect(false).build());

                quiz.getStepMap().put("1", step);

                content = quiz;
                break;

        }
        conversation.setContent(content);

        this.conversationMap.put(request.getFromSender(), conversation);
        return conversation;
    }

}
