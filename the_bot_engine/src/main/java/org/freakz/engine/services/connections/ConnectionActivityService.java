package org.freakz.engine.services.connections;

import org.freakz.common.model.connectionmanager.GetChannelActivityResponse;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.ActivityResponse;
import org.freakz.engine.services.api.AbstractService;
import org.freakz.engine.services.api.ServiceMessageHandler;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.api.ServiceRequestType;
import org.springframework.context.ApplicationContext;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.ConnectionActivityService)
public class ConnectionActivityService extends AbstractService {

  @Override
  public void initializeService(ConfigService configService) {
  }

  @Override
  public <T extends org.freakz.engine.services.api.ServiceResponse> ActivityResponse handleServiceRequest(ServiceRequest request) {
    ApplicationContext applicationContext = request.getApplicationContext();
    ConnectionManagerService connections = applicationContext.getBean(ConnectionManagerService.class);
    ActivityResponse response = ActivityResponse.builder().build();
    if (connections != null) {
      GetChannelActivityResponse activity = connections.getChannelActivity();
      response.setStatus("OK");
      response.setChannels(activity.getChannels());
    } else {
      response.setStatus("NOK: failed!");
    }
    return response;
  }
}
