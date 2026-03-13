package org.freakz.engine.services.channel;

import org.freakz.engine.dto.OPRequestResponse;
import org.freakz.engine.services.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@SpringServiceMethodHandler
public class ChannelOperationsService {

  private static final Logger log = LoggerFactory.getLogger(ChannelOperationsService.class);

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.ChannelOpRequest)
  public <T extends ServiceResponse> ServiceResponse handleChannelOp(ServiceRequest request) {
    OPRequestResponse response = OPRequestResponse.builder().build();
    response.setResponse("OPs!?");
    return response;
  }
}
