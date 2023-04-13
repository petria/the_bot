package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

@HokanCommandHandler
public class TestNoInitParamsCmd extends AbstractCmd {
    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        return getCommandClassName();
    }
}
