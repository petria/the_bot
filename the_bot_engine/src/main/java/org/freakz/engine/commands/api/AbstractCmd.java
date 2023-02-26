package org.freakz.engine.commands.api;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import lombok.Getter;
import org.apache.commons.cli.Options;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.CommandHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceRequestType;
import org.freakz.services.ServiceResponse;

public abstract class AbstractCmd implements HokanCmd {


    protected Options options = new Options();

    @Getter
    private JSAP jsap = new JSAP();

    private CommandHandler commandHandler;

    @Override
    public Options getOptions() {
        return this.options;
    }

    @Override
    public void validateRequestParameters(EngineRequest request) {

    }

    public String getCommandName() {
        return this.getClass().getSimpleName();
    }

    public String getName() {
        return this.getClass().getSimpleName().replaceAll("Cmd", "").toLowerCase();
    }

    public void abstractInitCommandOptions() throws NotImplementedException, JSAPException {
        initCommandOptions(this.jsap);
    }

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        throw new NotImplementedException("Comamnd handler must Override initCommandOptions(): " + getClass().getSimpleName());
    }

    @Override
    public void setCommandHandler(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public <T extends ServiceResponse> T doServiceRequest(EngineRequest engineRequest, ServiceRequestType serviceRequestType) {
        ServiceRequest request = ServiceRequest.builder()
                .engineRequest(engineRequest)
                .build();
        return commandHandler.getHokanServices().doServiceRequest(request, serviceRequestType);
    }

}
