package org.freakz.services.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.connectionmanager.GetConnectionMapResponse;
import org.freakz.config.ConfigService;
import org.freakz.dto.ConnectionsResponse;
import org.freakz.services.AbstractService;
import org.freakz.services.ServiceMessageHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceRequestType;
import org.freakz.services.ServiceResponse;
import org.springframework.context.ApplicationContext;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.ConnectionControlService)
public class ConnectionControlService  extends AbstractService {
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