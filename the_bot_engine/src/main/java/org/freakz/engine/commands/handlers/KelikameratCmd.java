package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.KelikameratResponse;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceResponse;
import org.springframework.stereotype.Component;

@Component
public class KelikameratCmd extends AbstractCmd {

    @Override
    void executeCommand(EngineRequest engineRequest) {
        ServiceRequest request = null;
        ServiceResponse<KelikameratResponse> serviceResponse = hokanServices.handleServiceRequest(request);
        Object response = serviceResponse.getResponse();

    }
}
