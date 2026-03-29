package org.freakz.engine.services.connections;

import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.SendMessageByEchoToAliasResponse;
import org.freakz.engine.services.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_ECHO_TO_ALIAS;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_MESSAGE;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.SendMessageByEchoToAlias)
public class SendMessagesToConnectionsService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(SendMessagesToConnectionsService.class);

  @Override
  public void initializeService(ConfigService configService) throws Exception {
  }

  @Override
  public <T extends ServiceResponse> SendMessageByEchoToAliasResponse handleServiceRequest(
      ServiceRequest request) {
    ApplicationContext applicationContext = request.getApplicationContext();
    ConnectionManagerService cms = applicationContext.getBean(ConnectionManagerService.class);
    SendMessageByEchoToAliasResponse response = SendMessageByEchoToAliasResponse.builder().build();

    String echoToAlias = request.getResults().getString(ARG_ECHO_TO_ALIAS);
    String message = request.getResults().getString(ARG_MESSAGE);

    org.freakz.common.model.connectionmanager.SendMessageByEchoToAliasResponse targetResponse =
        cms.sendMessageByEchoToAlias(message, echoToAlias);

    response.setStatus("OK");
    response.setSendTo(targetResponse.getSentTo());

    return response;
  }
}
