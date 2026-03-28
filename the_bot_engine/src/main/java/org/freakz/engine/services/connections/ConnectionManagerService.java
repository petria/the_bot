package org.freakz.engine.services.connections;

import org.freakz.common.model.connectionmanager.*;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.common.spring.rest.RestMessageSendClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ConnectionManagerService {

  private static final Logger log = LoggerFactory.getLogger(ConnectionManagerService.class);


  @Autowired
  private RestConnectionManagerClient connectionManagerClient;

  @Autowired
  private RestMessageSendClient messageSendClient;

  @Autowired
  private JsonMapper objectMapper;

  public GetConnectionMapResponse getConnectionsMap() {
    GetConnectionMapResponse connectionMap = connectionManagerClient.getConnectionMap();
//    Response response = client.getConnectionMap();
//    Optional<GetConnectionMapResponse> responseBody = FeignUtils.getResponseBody(response, GetConnectionMapResponse.class, objectMapper);
//    return responseBody.get();
    return connectionMap;
  }

  public SendMessageByEchoToAliasResponse sendMessageByEchoToAlias(String message, String echoToAlias) {
//        log.debug("Send!");
    SendMessageByEchoToAliasRequest request
        = SendMessageByEchoToAliasRequest.builder()
        .message(message)
        .echoToAlias(echoToAlias)
        .build();

    ResponseEntity<SendMessageByEchoToAliasResponse> response = messageSendClient.sendMessageByEchoToAlias(request);
//    Response response = messageSendClient.sendMessageByEchoToAlias(request);
//    Optional<SendMessageByEchoToAliasResponse> responseBody = FeignUtils.getResponseBody(response, SendMessageByEchoToAliasResponse.class, objectMapper);
    //  return responseBody.get();
    return response.getBody();
  }


  public ChannelUsersByEchoToAliasResponse getChannelUsersByEchoToAlias(String echoToAlias) {
    ChannelUsersByEchoToAliasRequest request
        = ChannelUsersByEchoToAliasRequest.builder()
        .echoToAlias(echoToAlias)
        .build();

    ResponseEntity<ChannelUsersByEchoToAliasResponse> response = connectionManagerClient.getChannelUsersByEchoToAlias(request);

//    Response response = connectionManagerClient.getChannelUsersByEchoToAlias(request);
//    Optional<ChannelUsersByEchoToAliasResponse> responseBody = FeignUtils.getResponseBody(response, ChannelUsersByEchoToAliasResponse.class, objectMapper);
//    return responseBody.get();

    return response.getBody();
  }
}
