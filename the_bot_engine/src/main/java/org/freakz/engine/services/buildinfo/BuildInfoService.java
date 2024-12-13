package org.freakz.engine.services.buildinfo;

import org.freakz.engine.services.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
@SpringServiceMethodHandler
public class BuildInfoService extends AbstractSpringService {

  @Autowired(required = false)
  private BuildProperties bp;

  @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.BuildInfoQuery)
  public <T extends ServiceResponse> ServiceResponse handleBuildInfoQuery(ServiceRequest request) {
    String buildInfo;
    if (bp != null) {
      String details =
          String.format(
              "java version: %s - os.arch: %s - os.name: %s - os.version: %s",
              bp.get("java.version"), bp.get("os.arch"), bp.get("os.name"), bp.get("os.version"));

      buildInfo =
          String.format(
              "BuildInfo: %s %s %s - %s",
              bp.getGroup(), bp.getArtifact(), bp.getVersion(), details);

    } else {
      buildInfo = "BuildInfo: n/a";
    }

    ServiceResponse response = new ServiceResponse();
    response.setStatus(buildInfo);
    return response;
  }
}
