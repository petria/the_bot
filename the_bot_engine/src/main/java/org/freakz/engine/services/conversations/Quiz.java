package org.freakz.engine.services.conversations;

import org.freakz.common.model.engine.EngineRequest;

import java.util.HashMap;
import java.util.Map;

public class Quiz extends ConversationContent {

  private String topic;

  private Map<String, QuizStep> stepMap = new HashMap<>();

  public Quiz() {
  }

  public Quiz(String topic) {
    this.topic = topic;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public Map<String, QuizStep> getStepMap() {
    return stepMap;
  }

  public void setStepMap(Map<String, QuizStep> stepMap) {
    this.stepMap = stepMap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Quiz quiz = (Quiz) o;

    if (topic != null ? !topic.equals(quiz.topic) : quiz.topic != null) return false;
    return stepMap != null ? stepMap.equals(quiz.stepMap) : quiz.stepMap == null;
  }

  @Override
  public int hashCode() {
    int result = topic != null ? topic.hashCode() : 0;
    result = 31 * result + (stepMap != null ? stepMap.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Quiz{" +
        "topic='" + topic + '\'' +
        ", stepMap=" + stepMap +
        '}';
  }

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

  static class QuizStep {

    private String step;

    private Map<String, QuizQuestion> questionMap;

    public QuizStep() {
    }

    public QuizStep(String step, Map<String, QuizQuestion> questionMap) {
      this.step = step;
      this.questionMap = questionMap;
    }

    public static Builder builder() {
      return new Builder();
    }

    public String getStep() {
      return step;
    }

    public void setStep(String step) {
      this.step = step;
    }

    public Map<String, QuizQuestion> getQuestionMap() {
      return questionMap;
    }

    public void setQuestionMap(Map<String, QuizQuestion> questionMap) {
      this.questionMap = questionMap;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      QuizStep quizStep = (QuizStep) o;

      if (step != null ? !step.equals(quizStep.step) : quizStep.step != null) return false;
      return questionMap != null ? questionMap.equals(quizStep.questionMap) : quizStep.questionMap == null;
    }

    @Override
    public int hashCode() {
      int result = step != null ? step.hashCode() : 0;
      result = 31 * result + (questionMap != null ? questionMap.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "QuizStep{" +
          "step='" + step + '\'' +
          ", questionMap=" + questionMap +
          '}';
    }

    public void addQuizQuestion(String key, QuizQuestion question) {
      questionMap.put(key, question);
    }

    public static class Builder {
      private String step;
      private Map<String, QuizQuestion> questionMap;

      Builder() {
      }

      public Builder step(String step) {
        this.step = step;
        return this;
      }

      public Builder questionMap(Map<String, QuizQuestion> questionMap) {
        this.questionMap = questionMap;
        return this;
      }

      public QuizStep build() {
        return new QuizStep(step, questionMap);
      }
    }

  }


  static class QuizQuestion {
    boolean isCorrect;
    private String question;

    public QuizQuestion() {
    }

    public QuizQuestion(String question, boolean isCorrect) {
      this.question = question;
      this.isCorrect = isCorrect;
    }

    public static Builder builder() {

      return new Builder();

    }

    public String getQuestion() {
      return question;
    }

    public void setQuestion(String question) {
      this.question = question;
    }

    public boolean isCorrect() {
      return isCorrect;
    }

    public void setCorrect(boolean correct) {
      isCorrect = correct;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      QuizQuestion that = (QuizQuestion) o;

      if (isCorrect != that.isCorrect) return false;
      return question != null ? question.equals(that.question) : that.question == null;
    }

    @Override
    public int hashCode() {
      int result = question != null ? question.hashCode() : 0;
      result = 31 * result + (isCorrect ? 1 : 0);
      return result;
    }

    @Override

    public String toString() {

      return "QuizQuestion{" +

          "question='" + question + '\'' +

          ", isCorrect=" + isCorrect +

          '}';

    }

    public static class Builder {

      private String question;

      private boolean isCorrect;


      Builder() {

      }


      public Builder question(String question) {

        this.question = question;

        return this;

      }


      public Builder isCorrect(boolean isCorrect) {

        this.isCorrect = isCorrect;

        return this;

      }


      public QuizQuestion build() {

        return new QuizQuestion(question, isCorrect);

      }

    }
  }

}
