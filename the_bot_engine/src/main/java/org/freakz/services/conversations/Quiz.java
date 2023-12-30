package org.freakz.services.conversations;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.common.model.engine.EngineRequest;

import java.util.HashMap;
import java.util.Map;


@Data
@EqualsAndHashCode(callSuper = false)
public class Quiz extends ConversationContent {

    private String topic;

    private Map<String, QuizStep> stepMap = new HashMap<>();

    @Override
    public String handleConversation(EngineRequest request, int state) {
        StringBuilder sb = new StringBuilder();
        QuizStep step = stepMap.get(String.valueOf(state));
        if (step != null) {
            sb.append(step.getStep()).append("\n");
            int q = 1;
            while (true) {
                QuizQuestion question = step.getQuestionMap().get(String.valueOf(q));
                if (question == null) {
                    break;
                }
                sb.append(q);
                sb.append(") ");
                sb.append(question.getQuestion());
                sb.append("\n");
                q++;
            }
        }
        return sb.toString();
    }

    public Quiz(String topic) {
        this.topic = topic;
    }

    @Builder
    @Data
    static class QuizStep {

        private String step;

        private Map<String, QuizQuestion> questionMap;


        public void addQuizQuestion(String key, QuizQuestion question) {
            questionMap.put(key, question);
        }

    }


    @Builder
    @Data
    static class QuizQuestion {
        private String question;
        boolean isCorrect;
    }

}
