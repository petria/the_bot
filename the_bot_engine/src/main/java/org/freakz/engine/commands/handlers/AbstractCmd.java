package org.freakz.engine.commands.handlers;

import org.apache.commons.cli.Options;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.services.HokanServices;
import org.freakz.services.RequestHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceResponse;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCmd implements HokanCmd {

    private HokanServices hokanServices;

    private Options options = new Options();

    //    @Override
    public List<String> getAliases() {
        return new ArrayList<>();
    }

    public String getName() {

        String name = this.getClass().getSimpleName();
        if (name.endsWith("Cmd")) {
            name = name.replaceAll("Cmd", "");
        }
        return name;
    }

    public abstract String executeCommand(EngineRequest request);

    public <T extends ServiceResponse> T test(ServiceRequest request, RequestHandler requestHandler) {
        return hokanServices.test(request, requestHandler);
    }

}
