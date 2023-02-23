package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.time.LocalDateTime;


@HokanCommandHandler
public class PingCmd extends AbstractCmd {
    @Override
    public String executeCommand(EngineRequest request) {
        return "pong: " + LocalDateTime.now();
    }
}
