package org.freakz.engine.services.connections;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.connectionmanager.*;
import org.freakz.common.util.FeignUtils;
import org.freakz.engine.clients.ConnectionManagerClient;
import org.freakz.engine.clients.MessageSendClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConnectionManagerService {


    @Autowired
    private ConnectionManagerClient client;

    @Autowired
    private MessageSendClient messageSendClient;

    @Autowired
    private ObjectMapper objectMapper;

    public GetConnectionMapResponse getConnectionsMap() {
        Response response = client.getConnectionMap();
        Optional<GetConnectionMapResponse> responseBody = FeignUtils.getResponseBody(response, GetConnectionMapResponse.class, objectMapper);
        return responseBody.get();
    }

    public SendMessageByTargetAliasResponse sendMessageByTargetAlias(String message, String targetAlias) {
//        log.debug("Send!");
        SendMessageByTargetAliasRequest request
                = SendMessageByTargetAliasRequest.builder()
                .message(message)
                .targetAlias(targetAlias)
                .build();
        Response response = messageSendClient.sendMessageByTargetAlias(request);
        Optional<SendMessageByTargetAliasResponse> responseBody = FeignUtils.getResponseBody(response, SendMessageByTargetAliasResponse.class, objectMapper);
        return responseBody.get();
    }

    public SendIrcRawMessageByTargetAliasResponse sendIrcRawMessageByTargetAlias(String message, String targetAlias) {
        SendIrcRawMessageByTargetAliasRequest request
                = SendIrcRawMessageByTargetAliasRequest.builder()
                .message(message)
                .targetAlias(targetAlias)
                .build();
        Response response = messageSendClient.sendIrcRawMessageByTargetAlias(request);
        Optional<SendIrcRawMessageByTargetAliasResponse> responseBody = FeignUtils.getResponseBody(response, SendIrcRawMessageByTargetAliasResponse.class, objectMapper);
        return responseBody.get();
    }

    public ChannelUsersByTargetAliasResponse getChannelUsersByTargetAlias(String targetAlias) {
        ChannelUsersByTargetAliasRequest request
                = ChannelUsersByTargetAliasRequest.builder()
                .targetAlias(targetAlias)
                .build();
        Response response = client.getChannelUsersByTargetAlias(request);
        Optional<ChannelUsersByTargetAliasResponse> responseBody = FeignUtils.getResponseBody(response, ChannelUsersByTargetAliasResponse.class, objectMapper);
        return responseBody.get();
    }
}
