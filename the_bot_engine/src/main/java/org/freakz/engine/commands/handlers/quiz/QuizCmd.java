package org.freakz.engine.commands.handlers.quiz;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.QuizStartResponse;
import org.freakz.engine.services.api.ServiceRequestType;


@HokanCommandHandler
public class QuizCmd extends AbstractCmd {


    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("Quiz test.");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {

        QuizStartResponse response = doServiceRequest(request, results, ServiceRequestType.QuizStartRequest);

        return "QuizStartResponse: " + response;
    }
}
