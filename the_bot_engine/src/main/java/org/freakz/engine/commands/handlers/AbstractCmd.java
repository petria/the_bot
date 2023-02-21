package org.freakz.engine.commands.handlers;

import org.apache.commons.cli.Options;
import org.freakz.engine.commands.CommandHandler;
import org.freakz.services.RequestHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceResponse;

public abstract class AbstractCmd implements HokanCmd {


    private Options options = new Options();
    private CommandHandler commandHandler;

    @Override
    public void setCommandHandler(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public <T extends ServiceResponse> T doServiceRequest(ServiceRequest request, RequestHandler requestHandler) {
        return commandHandler.getHokanServices().doServiceRequest(request, requestHandler);
    }

}
