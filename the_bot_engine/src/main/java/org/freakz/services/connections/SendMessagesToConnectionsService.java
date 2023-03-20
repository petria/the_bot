package org.freakz.services.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.connectionmanager.SendMessageByTargetAliasResponse;
import org.freakz.config.ConfigService;
import org.freakz.dto.SendMessageByTargetResponse;
import org.freakz.services.AbstractService;
import org.freakz.services.ServiceMessageHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceRequestType;
import org.freakz.services.ServiceResponse;
import org.springframework.context.ApplicationContext;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_MESSAGE;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_TARGET_ALIAS;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.SendMessageByTargetAlias)
public class SendMessagesToConnectionsService extends AbstractService {
    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @Override
    public <T extends ServiceResponse> SendMessageByTargetResponse handleServiceRequest(ServiceRequest request) {
        ApplicationContext applicationContext = request.getApplicationContext();
        ConnectionManagerService cms = applicationContext.getBean(ConnectionManagerService.class);
        SendMessageByTargetResponse response = SendMessageByTargetResponse.builder().build();

        String targetAlias = request.getResults().getString(ARG_TARGET_ALIAS);
        String message = request.getResults().getString(ARG_MESSAGE);

        SendMessageByTargetAliasResponse targetResponse = cms.sendMessageByTargetAlias(message, targetAlias);

        response.setStatus("OK");
        response.setSendTo(targetResponse.getSentTo());

        return response;
    }
}
