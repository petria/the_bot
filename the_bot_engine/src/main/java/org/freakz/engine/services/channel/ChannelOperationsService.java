package org.freakz.engine.services.channel;

import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.dto.OPRequestResponse;
import org.freakz.engine.services.api.*;
import org.springframework.stereotype.Service;

@Service
@SpringServiceMethodHandler
@Slf4j
public class ChannelOperationsService {


    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.ChannelOpRequest)
    public <T extends ServiceResponse> ServiceResponse handleChannelOp(ServiceRequest request) {
        OPRequestResponse response = OPRequestResponse.builder().build();
        response.setResponse("OPs!?");
        return response;
    }
}