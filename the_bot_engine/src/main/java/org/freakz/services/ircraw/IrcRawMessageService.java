package org.freakz.services.ircraw;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.connectionmanager.SendIrcRawMessageByTargetAliasResponse;
import org.freakz.config.ConfigService;
import org.freakz.dto.IrcRawMessageResponse;
import org.freakz.services.*;
import org.freakz.services.connections.ConnectionManagerService;
import org.springframework.context.ApplicationContext;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_MESSAGE;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_TARGET_ALIAS;

@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.IrcRawMessage)
public class IrcRawMessageService extends AbstractService {

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @Override
    public <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request) {
        ApplicationContext applicationContext = request.getApplicationContext();
        ConnectionManagerService cms = applicationContext.getBean(ConnectionManagerService.class);

        String targetAlias = request.getResults().getString(ARG_TARGET_ALIAS);
        String message = request.getResults().getString(ARG_MESSAGE);

        log.debug(">> raw message send");
        SendIrcRawMessageByTargetAliasResponse serviceResponse = cms.sendIrcRawMessageByTargetAlias(message, targetAlias);
        log.debug("<< got reply: {}", serviceResponse);

        String serverResponse;
        if (serviceResponse.getSentTo().startsWith("NOK")) {
            serverResponse = serviceResponse.getSentTo();
        } else {
            serverResponse = serviceResponse.getServerResponse();
        }



        IrcRawMessageResponse response
                = IrcRawMessageResponse.builder()
                .ircServerResponse(serverResponse)
                .build();

        return response;
    }
}
