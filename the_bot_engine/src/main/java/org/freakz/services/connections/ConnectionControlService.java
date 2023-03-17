package org.freakz.services.connections;

import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.services.AbstractService;
import org.freakz.services.ServiceMessageHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceRequestType;
import org.freakz.services.ServiceResponse;
import org.springframework.context.ApplicationContext;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.KelikameratService)

public class ConnectionControlService  extends AbstractService {
    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @Override
    public <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request) {
        ApplicationContext applicationContext = request.getApplicationContext();
        ConnectionControlService connections = applicationContext.getBean(ConnectionControlService.class);
        if (connections != null) {

        }

        return null;
    }
}
