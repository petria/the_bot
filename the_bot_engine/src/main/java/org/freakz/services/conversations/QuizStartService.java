package org.freakz.services.conversations;


import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.dto.QuizStartResponse;
import org.freakz.services.api.*;
import org.springframework.context.ApplicationContext;

@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.QuizStartRequest)
public class QuizStartService extends AbstractService {

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @Override
    public <T extends ServiceResponse> QuizStartResponse handleServiceRequest(ServiceRequest request) {

        ApplicationContext applicationContext = request.getApplicationContext();
        ConversationsService service = applicationContext.getBean(ConversationsService.class);


        QuizStartResponse response = new QuizStartResponse();
        Conversation conversation = service.createConversation(request.getEngineRequest(), ConversationType.QUIZ);
        if (conversation != null) {
            Quiz quiz = (Quiz) conversation.getContent();
            response.setResponse("Quiz started: " + quiz.getTopic());
        } else {
            response.setResponse("Quiz already in progress.");
        }

        return response;
    }
}
