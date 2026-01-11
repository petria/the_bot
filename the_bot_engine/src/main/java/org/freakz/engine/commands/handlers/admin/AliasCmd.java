package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HokanCommandHandler
@HokanAdminCommand
public class AliasCmd extends AbstractCmd {

  private static final Logger log = LoggerFactory.getLogger(AliasCmd.class);

  @Override
  public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
    jsap.setHelp("List command aliases.");
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    StringBuilder sb = new StringBuilder("Alias list:\n");
    for (String alias : getBotEngine().getCommandHandlerLoader().getHandlerAliasMap().keySet()) {
      HandlerAlias ha = getBotEngine().getCommandHandlerLoader().getHandlerAliasMap().get(alias);
      sb.append(String.format("%s = %s\n", ha.getAlias(), ha.getTarget()));
    }
    return sb.toString();
  }
}
