package org.freakz.engine.commands.handlers.weather;

import com.martiansoftware.jsap.*;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.commands.util.WeatherUtils;
import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;

import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.*;

@HokanCommandHandler
@Slf4j
public class WeatherCmd extends AbstractCmd {

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {

    jsap.setHelp("Query weather using weatherapi.com services.");

    Switch astronomy = new Switch(ARG_ASTRONOMY).setLongFlag("astronomy").setShortFlag('a');
    jsap.registerParameter(astronomy);

    Switch feelsLike = new Switch(ARG_FEELS_LIKE).setLongFlag("feelsLike").setShortFlag('f');
    jsap.registerParameter(feelsLike);

    Switch verbose = new Switch(ARG_VERBOSE).setLongFlag("verbose").setShortFlag('v');
    jsap.registerParameter(verbose);

    UnflaggedOption opt =
        new UnflaggedOption(ARG_PLACE).setDefault("Oulu").setRequired(true).setGreedy(false);
    jsap.registerParameter(opt);
  }

  @Override
  public List<HandlerAlias> getAliases(String botName) {
    List<HandlerAlias> list = new ArrayList<>();
    list.add(createWithArgsAlias("!saa", "!weather"));
    list.add(createWithArgsAlias("!sää", "!weather"));
    list.add(createWithArgsAlias("!foreca", "!weather"));
    list.add(createWithArgsAlias("!keli", "!weather"));

    return list;
  }

  @Override
  public String executeCommand(EngineRequest engineRequest, JSAPResult results) {
    boolean verbose = results.getBoolean(ARG_VERBOSE);
    boolean doAstronomy = results.getBoolean(ARG_ASTRONOMY);
    boolean doFeelsLike = results.getBoolean(ARG_FEELS_LIKE);

    WeatherAPIResponse response =
        doServiceRequestMethods(engineRequest, results, ServiceRequestType.WeatherAPIService);
    if (response.getStatus().startsWith("OK")) {
      ForecastResponse r = response.getForecastResponseModel();
      String time = WeatherUtils.formatTime(r);
      String name = WeatherUtils.formatName(r, verbose);
      String feelsLike = WeatherUtils.formatFeelsLike(r, doFeelsLike);
      String astronomy = WeatherUtils.formatAstronomy(response.getAstronomyResponse(), doAstronomy);
      return String.format(
          "%s: %s, %s°C%s%s", name, time, r.current().temp_c(), feelsLike, astronomy);

    } else {
      return String.format(
          "%s: %s", results.getString(ARG_PLACE), response.getErrorResponse().error().message());
    }
  }

}
