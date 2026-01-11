package org.freakz.engine.services.conversations;

import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.QuizStartResponse;
import org.freakz.engine.services.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.QuizStartRequest)
public class QuizStartService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(QuizStartService.class);

  @Override
  public void initializeService(ConfigService configService) throws Exception {
  }

  @Override
  public <T extends ServiceResponse> QuizStartResponse handleServiceRequest(
      ServiceRequest request) {

    ApplicationContext applicationContext = request.getApplicationContext();
    ConversationsService service = applicationContext.getBean(ConversationsService.class);

    QuizStartResponse response = new QuizStartResponse();
    Conversation conversation =
        service.createConversation(request.getEngineRequest(), ConversationType.QUIZ);
    if (conversation != null) {
      Quiz quiz = (Quiz) conversation.getContent();
      response.setResponse("Quiz started: " + quiz.getTopic());
    } else {
      response.setResponse("Quiz already in progress.");
    }

    return response;
  }
}
