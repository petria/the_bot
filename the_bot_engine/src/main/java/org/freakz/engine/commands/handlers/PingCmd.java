package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import java.time.LocalDateTime;


@HokanCommandHandler
public class PingCmd extends AbstractCmd {


    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {

    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        return "pOnG: " + LocalDateTime.now();
    }
}
