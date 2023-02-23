package org.freakz.engine.commands.api;

import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.CommandHandler;

public interface HokanCmd {

    void setCommandHandler(CommandHandler commandHandler);

    String executeCommand(EngineRequest request);

//    List<String> getAliases();

}
