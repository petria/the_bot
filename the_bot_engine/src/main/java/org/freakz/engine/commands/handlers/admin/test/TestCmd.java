package org.freakz.engine.commands.handlers.admin.test;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.api.ServiceResponse;

@HokanCommandHandler
@HokanAdminCommand
public class TestCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("Test multiple service handlers command");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {

        ServiceResponse response1 = doServiceRequest(request, results, ServiceRequestType.TestService1);
        ServiceResponse response2 = doServiceRequestMethods(request, results, ServiceRequestType.TestService2);
        ServiceResponse response3 = doServiceRequestMethods(request, results, ServiceRequestType.TestService3);

        return "Testing: response1=" + response1 + " -- response2=" + response2 + " -- response3=" + response3;

    }
}
