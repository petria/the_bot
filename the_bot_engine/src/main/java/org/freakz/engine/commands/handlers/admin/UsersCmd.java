package org.freakz.engine.commands.handlers.admin;


import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.UsersResponse;
import org.freakz.engine.services.api.ServiceRequestType;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class UsersCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("List registered bot users.");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        UsersResponse response = doServiceRequest(request, results, ServiceRequestType.UsersListService);
        sb().append("== Users\n");
        response.getUserList().forEach(user -> {
            format(" [%d] %s ", user.getId(), user.getName());
        });
        return sb().toString();
    }
}
