package org.freakz.engine.commands.handlers.irc;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.dto.IrcOPResponse;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import java.time.LocalDateTime;


//@HokanCommandHandler
public class OpCmd extends AbstractCmd {


    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("Request Operator rights on IRC channel.");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {

        IrcOPResponse response = doServiceRequest(request, results, ServiceRequestType.IrcOpRequest);

        return "pong: " + LocalDateTime.now();
    }
}
