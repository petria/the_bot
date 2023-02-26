package org.freakz.engine.commands.handlers;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

@HokanCommandHandler
public class TestNoInitParamsCmd extends AbstractCmd {
    @Override
    public String executeCommand(EngineRequest request) {
        return getCommandClassName();
    }
}
