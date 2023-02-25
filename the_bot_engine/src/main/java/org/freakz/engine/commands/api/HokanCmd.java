package org.freakz.engine.commands.api;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import org.apache.commons.cli.Options;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.CommandHandler;

public interface HokanCmd {

    Options getOptions();

    void setCommandHandler(CommandHandler commandHandler);

    void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException;

    void validateRequestParameters(EngineRequest request);

    String executeCommand(EngineRequest request);

//    List<String> getAliases();

}
