package org.freakz.engine.services.buildinfo;


import lombok.ToString;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
@SpringServiceMethodHandler
public class BuildInfoService extends AbstractService {

    @Autowired
    private BuildProperties buildProperties;

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @ToString
    class ToStringWrapper {
        BuildProperties buildProperties;
    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.BuildInfoQuery)
    public <T extends ServiceResponse> ServiceResponse handleBuildInfoQuery(ServiceRequest request) {
        ToStringWrapper wrapper = new ToStringWrapper();
        wrapper.buildProperties = this.buildProperties;

        ServiceResponse response = new ServiceResponse();
        response.setStatus(wrapper.toString());
        return response;

    }


}
