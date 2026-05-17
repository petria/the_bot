package org.freakz.engine.commands.testprovider;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.api.AbstractCmd;

public class SampleCmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Test provider sample command.");
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    return "sample";
  }
}
