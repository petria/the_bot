package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.services.HokanServices;
import org.freakz.services.RequestHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceResponse;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractCmd implements HokanCmd {

    @Autowired
    private HokanServices hokanServices;

    public String getName() {
        String name = this.getClass().getSimpleName();
        if (name.endsWith("Cmd")) {
            name = name.replaceAll("Cmd", "");
        }
        return name;
    }

    public abstract void executeCommand(EngineRequest request);

    public <T extends ServiceResponse> T doService(ServiceRequest request, Class<T> clazz, RequestHandler requestHandler) {
        return hokanServices.doService(request, clazz, requestHandler);
    }
}
