package org.freakz.engine.commands.handlers.weather;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.services.water.WaterTemperatureCommandService;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

@HokanCommandHandler
public class WaterCmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {
    jsap.setHelp("Read the current river water temperature from Finnish water monitoring charts.");
    jsap.registerParameter(new UnflaggedOption(ARG_PLACE).setRequired(true).setGreedy(true));
  }

  @Override
  public String executeCommand(EngineRequest request, JSAPResult results) {
    WaterTemperatureCommandService service = getBotEngine().getHokanServices().getApplicationContext()
        .getBean(WaterTemperatureCommandService.class);
    service.ask(request, String.join(" ", results.getStringArray(ARG_PLACE)));
    return null;
  }
}
