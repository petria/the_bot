package org.freakz.services.conversations;


import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.dto.QuizStartResponse;
import org.freakz.services.*;
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

        Conversation conversation = service.createConversation(request.getEngineRequest(), ConversationType.QUIZ);

        QuizStartResponse response = new QuizStartResponse();
        response.setResponse("Created quiz: " + conversation.toString());

        return response;
    }
}
