package org.freakz.engine.services.buildinfo;

import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
@SpringServiceMethodHandler
public class BuildInfoService extends AbstractService {

    @Autowired
    private BuildProperties bp;

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.BuildInfoQuery)
    public <T extends ServiceResponse> ServiceResponse handleBuildInfoQuery(ServiceRequest request) {

        String buildInfo = String.format("BuildInfo: %s %s %s",
                bp.getGroup(),
                bp.getArtifact(),
                bp.getVersion()
        );
        ServiceResponse response = new ServiceResponse();
        response.setStatus(buildInfo);
        return response;
    }

}
