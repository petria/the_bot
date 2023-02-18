package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.services.HokanServices;
import org.freakz.services.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractCmd implements HokanCmd {

    @Autowired
    HokanServices hokanServices;

    public String getName() {
        String name = this.getClass().getSimpleName();
        if (name.endsWith("Cmd")) {
            name = name.replaceAll("Cmd", "");
        }
        return name;
    }

    abstract void executeCommand(EngineRequest request);

    <T> T doService(ServiceRequest request, Class<T> clazz) {
        T response = null;

        return response;
    }
}
