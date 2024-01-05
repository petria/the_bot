package org.freakz.engine.services.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.connectionmanager.GetConnectionMapResponse;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.ConnectionsResponse;
import org.freakz.engine.services.api.*;
import org.springframework.context.ApplicationContext;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.ConnectionControlService)
public class ConnectionControlService extends AbstractService {
    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @Override
    public <T extends ServiceResponse> ConnectionsResponse handleServiceRequest(ServiceRequest request) {
        ApplicationContext applicationContext = request.getApplicationContext();
        ConnectionManagerService connections = applicationContext.getBean(ConnectionManagerService.class);
        ConnectionsResponse response = ConnectionsResponse.builder().build();
        if (connections != null) {
            GetConnectionMapResponse connectionsMap = connections.getConnectionsMap();
            response.setStatus("OK");
            response.setConnectionMap(connectionsMap.getConnectionMap());
        } else {
            response.setStatus("NOK: failed!");
        }

        return response;
    }
}
