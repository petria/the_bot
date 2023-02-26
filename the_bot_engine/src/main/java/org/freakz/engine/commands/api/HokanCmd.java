package org.freakz.engine.commands.api;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.CommandHandler;

public interface HokanCmd {

    void setCommandHandler(CommandHandler commandHandler);

    void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException;

    String executeCommand(EngineRequest request);


}
