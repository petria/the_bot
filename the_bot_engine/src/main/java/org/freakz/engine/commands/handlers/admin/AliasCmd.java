package org.freakz.engine.commands.handlers.admin;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
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
    getBotEngine().getCommandHandlerLoader().getHandlerAliasMap().values().stream()
        .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.getAlias(), right.getAlias()))
        .forEach(ha -> sb.append(String.format(
            "%s%s = %s\n",
            ha.getAlias(),
            ha.isWithArgs() ? " + args" : "",
            ha.getTarget())));
    return sb.toString();
  }
}
