package org.freakz.engine.services.connections;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  public SendMessageByTargetAliasResponse sendMessageByTargetAlias(String message, String targetAlias) {
//        log.debug("Send!");
    SendMessageByTargetAliasRequest request
        = SendMessageByTargetAliasRequest.builder()
        .message(message)
        .targetAlias(targetAlias)
        .build();

    ResponseEntity<SendMessageByTargetAliasResponse> response = messageSendClient.sendMessageByTargetAlias(request);
//    Response response = messageSendClient.sendMessageByTargetAlias(request);
//    Optional<SendMessageByTargetAliasResponse> responseBody = FeignUtils.getResponseBody(response, SendMessageByTargetAliasResponse.class, objectMapper);
    //  return responseBody.get();
    return response.getBody();
  }

  public SendIrcRawMessageByTargetAliasResponse sendIrcRawMessageByTargetAlias(String message, String targetAlias) {
    SendIrcRawMessageByTargetAliasRequest request
        = SendIrcRawMessageByTargetAliasRequest.builder()
        .message(message)
        .targetAlias(targetAlias)
        .build();

    ResponseEntity<SendIrcRawMessageByTargetAliasResponse> response = messageSendClient.sendIrcRawMessageByTargetAlias(request);
//    Response response = messageSendClient.sendIrcRawMessageByTargetAlias(request);
//    Optional<SendIrcRawMessageByTargetAliasResponse> responseBody = FeignUtils.getResponseBody(response, SendIrcRawMessageByTargetAliasResponse.class, objectMapper);
//    return responseBody.get();
    return response.getBody();
  }

  public ChannelUsersByTargetAliasResponse getChannelUsersByTargetAlias(String targetAlias) {
    ChannelUsersByTargetAliasRequest request
        = ChannelUsersByTargetAliasRequest.builder()
        .targetAlias(targetAlias)
        .build();

    ResponseEntity<ChannelUsersByTargetAliasResponse> response = connectionManagerClient.getChannelUsersByTargetAlias(request);

//    Response response = connectionManagerClient.getChannelUsersByTargetAlias(request);
//    Optional<ChannelUsersByTargetAliasResponse> responseBody = FeignUtils.getResponseBody(response, ChannelUsersByTargetAliasResponse.class, objectMapper);
//    return responseBody.get();

    return response.getBody();
  }
}
