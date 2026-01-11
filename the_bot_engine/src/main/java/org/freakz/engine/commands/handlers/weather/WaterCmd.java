package org.freakz.engine.commands.handlers.weather;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.weather.WaterTemperatureResponse;
import org.freakz.engine.services.api.ServiceRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

@HokanCommandHandler
public class WaterCmd extends AbstractCmd {

  private static final Logger log = LoggerFactory.getLogger(WaterCmd.class);

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {

    jsap.setHelp("Query water temperature.");

    UnflaggedOption opt =
        new UnflaggedOption(ARG_PLACE).setDefault("Oulu").setRequired(true).setGreedy(false);
    jsap.registerParameter(opt);
  }

  @Override
  public List<HandlerAlias> getAliases(String botName) {
    List<HandlerAlias> list = new ArrayList<>();
    list.add(createWithArgsAlias("!vesi", "!water"));
    list.add(createWithArgsAlias("!joki", "!water"));
    return list;
  }

  @Override
  public String executeCommand(EngineRequest engineRequest, JSAPResult results) {

    WaterTemperatureResponse response =
        doServiceRequestMethods(engineRequest, results, ServiceRequestType.WaterTemperatureService);
    if (response.getStatus().startsWith("OK")) {
      return response.getWaterTemperature();
    } else {
      return String.format(
          "%s: %s", results.getString(ARG_PLACE), response.getStatus());
    }
  }

}
