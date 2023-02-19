package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.KelikameratResponse;
import org.freakz.services.ServiceRequest;
import org.springframework.stereotype.Component;

import static org.freakz.services.RequestHandler.KeliKameratService;

@Component
public class KelikameratCmd extends AbstractCmd {

    @Override
    public void executeCommand(EngineRequest engineRequest) {
        ServiceRequest request = ServiceRequest.builder()
                .engineRequest(engineRequest)
                .build();
//        doServiceRequest(request, RequestHandler.KeliKameratService);
//        KelikameratResponse kelikameratResponse = doService(request, KeliKameratService.getResponseClazz(), KeliKameratService);
        KelikameratResponse kelikameratResponse = test(request, KeliKameratService);

        int foo = 0;
    }
}
