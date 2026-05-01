package org.freakz.engine.services.connections;

import org.freakz.common.model.connectionmanager.*;
import org.freakz.common.spring.rest.RestConnectionManagerClient;
import org.freakz.common.spring.rest.RestMessageSendClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ConnectionManagerService {

  private static final Logger log = LoggerFactory.getLogger(ConnectionManagerService.class);


  @Autowired
  private RestConnectionManagerClient connectionManagerClient;

  @Autowired
  private RestMessageSendClient messageSendClient;


  public GetConnectionMapResponse getConnectionsMap() {
    GetConnectionMapResponse connectionMap = connectionManagerClient.getConnectionMap();
//    Response response = client.getConnectionMap();
//    Optional<GetConnectionMapResponse> responseBody = FeignUtils.getResponseBody(response, GetConnectionMapResponse.class, objectMapper);
//    return responseBody.get();
    return connectionMap;
  }

  public GetChannelActivityResponse getChannelActivity() {
    return connectionManagerClient.getChannelActivity();
  }

  public GetKnownChatChannelsResponse getKnownChannels() {
    return connectionManagerClient.getKnownChannels();
  }

  public GetKnownChatUsersResponse getKnownUsers(String query) {
    return connectionManagerClient.getKnownUsers(query);
  }

  public GetKnownUserTargetsResponse getKnownUserTargets(String query) {
    return connectionManagerClient.getKnownUserTargets(query);
  }

  public SendMessageByEchoToAliasResponse sendMessageByEchoToAlias(String message, String echoToAlias) {
    long startedAt = System.currentTimeMillis();
    log.debug(
        "ConnectionManagerService.sendMessageByEchoToAlias start echoToAlias={} messageLength={}",
        echoToAlias,
        message == null ? 0 : message.length()
    );
    SendMessageByEchoToAliasRequest request
        = SendMessageByEchoToAliasRequest.builder()
        .message(message)
        .echoToAlias(echoToAlias)
        .build();

    ResponseEntity<SendMessageByEchoToAliasResponse> response = messageSendClient.sendMessageByEchoToAlias(request);
    long durationMs = System.currentTimeMillis() - startedAt;
    SendMessageByEchoToAliasResponse body = response.getBody();
    log.debug(
        "ConnectionManagerService.sendMessageByEchoToAlias done echoToAlias={} durationMs={} status={} sentTo={}",
        echoToAlias,
        durationMs,
        response.getStatusCode(),
        body == null ? null : body.getSentTo()
    );
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
