package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.KelikameratResponse;
import org.freakz.services.RequestHandler;
import org.freakz.services.ServiceRequest;
import org.springframework.stereotype.Component;

@Component
public class KelikameratCmd extends AbstractCmd {

    @Override
    public void executeCommand(EngineRequest engineRequest) {
        ServiceRequest request = ServiceRequest.builder()
                .engineRequest(engineRequest)
                .build();
        KelikameratResponse kelikameratResponse = doService(request, KelikameratResponse.class, RequestHandler.KeliKameratService);
        int foo = 0;
    }
}
