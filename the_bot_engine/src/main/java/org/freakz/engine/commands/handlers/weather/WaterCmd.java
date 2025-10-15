package org.freakz.engine.commands.handlers.weather;

import com.martiansoftware.jsap.*;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.util.WeatherUtils;
import org.freakz.engine.dto.weather.WaterTemperatureResponse;
import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;

import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.*;

@HokanCommandHandler
@Slf4j
public class WaterCmd extends AbstractCmd {

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
      return "Water temperature response";
    } else {
      return String.format(
          "%s: %s", results.getString(ARG_PLACE), response.getWaterTemperature());
    }
  }

}
