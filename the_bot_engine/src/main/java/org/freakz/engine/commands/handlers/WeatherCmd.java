package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.KelikameratResponse;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;


@HokanCommandHandler
@Slf4j
public class WeatherCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Get weather for city.");

        UnflaggedOption opt = new UnflaggedOption(ARG_PLACE)
                .setDefault("Oulu")
                .setRequired(true)
                .setGreedy(false);

        jsap.registerParameter(opt);

    }

    @Override
    public String executeCommand(EngineRequest engineRequest) {

        KelikameratResponse data = doServiceRequest(engineRequest, ServiceRequestType.KelikameratService);
        if (data.getStatus().startsWith("OK")) {
            return data.getStatus();
        } else {
            return this.getClass().getSimpleName() + " error :: " + data.getStatus();
        }


//        return "weather reply!";
    }
}
