package org.freakz.engine.commands.api;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.Getter;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.services.api.ServiceRequest;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.Collections;
import java.util.List;

public abstract class AbstractCmd implements HokanCmd {


    private boolean adminCommand = false;

    @Getter
    private JSAP jsap = new JSAP();

    @Getter
    private BotEngine botEngine;

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
    public List<HandlerAlias> getAliases(String botName) {
        return Collections.emptyList();
    }

    public HandlerAlias createAlias(String alias, String target) {
        return HandlerAlias.builder().alias(alias).target(target).withArgs(false).build();
    }

    public HandlerAlias createWithArgsAlias(String alias, String target) {
        return HandlerAlias.builder().alias(alias).target(target).withArgs(true).build();
    }

    public HandlerAlias createToBotAliasWithArgs(String botName, String target) {
        return HandlerAlias.builder().alias(botName + ":").target(target).withArgs(true).build();
    }


    public void setBotEngine(BotEngine botEngine) {
        this.botEngine = botEngine;
    }

    public <T extends ServiceResponse> T doServiceRequest(EngineRequest engineRequest, JSAPResult results, ServiceRequestType serviceRequestType) {
        ServiceRequest request = ServiceRequest.builder()
                .engineRequest(engineRequest)
                .results(results)
                .build();
        return botEngine.getHokanServices().doServiceRequest(request, serviceRequestType);
    }

    public <T extends ServiceResponse> T doServiceRequestMethods(EngineRequest engineRequest, JSAPResult results, ServiceRequestType serviceRequestType) {
        ServiceRequest request = ServiceRequest.builder()
                .engineRequest(engineRequest)
                .results(results)
                .build();
        return botEngine.getHokanServices().doServiceRequestMethods(request, serviceRequestType);
    }
}
