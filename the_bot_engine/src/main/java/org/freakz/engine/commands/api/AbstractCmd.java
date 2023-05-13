package org.freakz.engine.commands.api;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.Getter;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.CommandHandler;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceRequestType;
import org.freakz.services.ServiceResponse;

import java.util.Collections;
import java.util.List;

public abstract class AbstractCmd implements HokanCmd {


    private boolean adminCommand = false;

    @Getter
    private JSAP jsap = new JSAP();

    @Getter
    private CommandHandler commandHandler;

    private final StringBuilder sb = new StringBuilder();

    public StringBuilder sb() {
        return sb;
    }

    public void format(String format, Object... args) {
        sb.append(String.format(format, args));
    }

    public String getCommandClassName() {
        return this.getClass().getSimpleName();
    }

    public String getCommandName() {
        return this.getClass().getSimpleName().replaceAll("Cmd", "").toLowerCase();
    }

    public void abstractInitCommandOptions() throws NotImplementedException, JSAPException {
        initCommandOptions(this.jsap);
    }

    @Override
    public boolean isAdminCommand() {
        return this.adminCommand;
    }

    @Override
    public void setIsAdminCommand(boolean isAdminCommand) {
        this.adminCommand = isAdminCommand;
    }

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        throw new NotImplementedException("Command handler must Override initCommandOptions(): " + getClass().getSimpleName());
    }

    @Override
    public List<HandlerAlias> getAliases() {
        return Collections.emptyList();
    }

    public HandlerAlias createAlias(String alias, String target) {
        return HandlerAlias.builder().alias(alias).target(target).build();
    }


    @Override
    public void setCommandHandler(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public <T extends ServiceResponse> T doServiceRequest(EngineRequest engineRequest, JSAPResult results, ServiceRequestType serviceRequestType) {
        ServiceRequest request = ServiceRequest.builder()
                .engineRequest(engineRequest)
                .results(results)
                .build();
        return commandHandler.getHokanServices().doServiceRequest(request, serviceRequestType);
    }

}
