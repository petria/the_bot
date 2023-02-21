package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;


//@HokanCommandHandler
public class PingCmd extends AbstractCmd {
    @Override
    public String executeCommand(EngineRequest request) {
        return "pong: ";
    }
}
