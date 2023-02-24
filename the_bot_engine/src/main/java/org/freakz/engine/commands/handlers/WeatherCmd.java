package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.KelikameratResponse;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;


@HokanCommandHandler
public class WeatherCmd extends AbstractCmd {

    @Override
    public String executeCommand(EngineRequest engineRequest) {

        KelikameratResponse data = doServiceRequest(engineRequest, ServiceRequestType.KelikameratService);
        if (data.getStatus().startsWith("OK")) {

        } else {
            return this.getClass().getSimpleName() + " error :: " + data.getStatus();
        }


        return "weather reply!";
    }
}
