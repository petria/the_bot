package org.freakz.engine.commands.api;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.exception.NotImplementedException;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.BotEngine;
import org.freakz.engine.commands.HandlerAlias;

import java.util.List;

public interface HokanCmd {

  void setBotEngine(BotEngine botEngine);

  void initCommandOptions(JSAP jsap) throws NotImplementedException, JSAPException;

  String executeCommand(EngineRequest request, JSAPResult results);

  List<HandlerAlias> getAliases(String botName);

  boolean isAdminCommand();

  void setIsAdminCommand(boolean isAdminCommand);
}
