package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.UsersResponse;
import org.freakz.engine.services.api.ServiceRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HokanCommandHandler
@HokanAdminCommand
public class UsersCmd extends AbstractCmd {

  private static final Logger log = LoggerFactory.getLogger(UsersCmd.class);

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("List registered bot users.");
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    UsersResponse response =
        doServiceRequest(request, results, ServiceRequestType.UsersListService);
    sb().append("== Users\n");
    response
        .getUserList()
        .forEach(
            user -> {
              format(" [%d] %s ", user.getId(), user.getName());
            });
    return sb().toString();
  }
}
