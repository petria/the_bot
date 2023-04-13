package org.freakz.engine.commands.handlers.admin;


import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanAdminCommand;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

@HokanCommandHandler
@HokanAdminCommand
@Slf4j
public class AliasCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException {
        jsap.setHelp("List command aliases.");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        StringBuilder sb = new StringBuilder("Alias list:\n");
        for (String alias : getCommandHandler().getCommandHandlerLoader().getHandlerAliasMap().keySet()) {
            HandlerAlias ha = getCommandHandler().getCommandHandlerLoader().getHandlerAliasMap().get(alias);
            sb.append(String.format("%s = %s\n", ha.getAlias(), ha.getTarget()));
        }
        return sb.toString();
    }
}
