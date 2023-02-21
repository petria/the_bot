package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.KelikameratResponse;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.services.ServiceRequest;

import static org.freakz.services.RequestHandler.KeliKameratService;

@HokanCommandHandler
public class WeatherCmd extends AbstractCmd {

    @Override
    public String executeCommand(EngineRequest engineRequest) {
        ServiceRequest request = ServiceRequest.builder()
                .engineRequest(engineRequest)
                .build();
//        doServiceRequest(request, RequestHandler.KeliKameratService);
//        KelikameratResponse kelikameratResponse = doService(request, KeliKameratService.getResponseClazz(), KeliKameratService);
        KelikameratResponse kelikameratResponse = doServiceRequest(request, KeliKameratService);

        int foo = 0;

        return "weather reply!";
    }
}
