package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.KelikameratResponse;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import static org.freakz.services.RequestHandler.KeliKameratService;

@HokanCommandHandler
public class WeatherCmd extends AbstractCmd {

    @Override
    public String executeCommand(EngineRequest engineRequest) {

        KelikameratResponse weatherData = doServiceRequest(engineRequest, KeliKameratService);

        return "weather reply!";
    }
}
