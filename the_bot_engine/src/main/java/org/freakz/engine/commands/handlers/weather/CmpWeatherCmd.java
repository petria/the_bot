package org.freakz.engine.commands.handlers.weather;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.CmpWeatherResponse;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

@HokanCommandHandler
public class CmpWeatherCmd extends AbstractCmd {

  private static final Logger log = LoggerFactory.getLogger(CmpWeatherCmd.class);

  @Override
  public void initCommandOptions(JSAP jsap) throws JSAPException {

    jsap.setHelp("Compares weather between cities.");

    UnflaggedOption opt = new UnflaggedOption(ARG_PLACE)
        .setList(true)
        .setRequired(true)
        .setGreedy(true);

    jsap.registerParameter(opt);

  }

  private String formatWeather(ForecastResponse response, String diff, int longestCityName) {

    String template = "%-" + longestCityName + "s %s %s%6.1f°C - %s";
    return String.format(
        template,
        response.location().name(),
        response.location().localtime().toLocalDate(),
        response.location().localtime().toLocalTime(),
        Double.parseDouble(response.current().temp_c()),
        diff);
  }

  private int findLongestCityNameLength(List<ForecastResponse> forecastResponses) {
    int longest = Integer.MIN_VALUE;
    for (ForecastResponse response : forecastResponses) {
      if (response.location().name().length() > longest) {
        longest = response.location().name().length();
      }
    }
    return longest;
  }

  @Override
  public String executeCommand(EngineRequest engineRequest, JSAPResult results) {

    String[] places = results.getStringArray(ARG_PLACE);


    if (places.length < 2) {
      return "It needs at least two arguments to compare the weather";
    }

    CmpWeatherResponse data = doServiceRequestMethods(engineRequest, results, ServiceRequestType.CmpWeatherService);

    if (data.getStatus().startsWith("OK")) {
      StringBuilder sb = new StringBuilder();

      if (data.getForecastResponses().isEmpty()) {
        sb.append("Check spelling, no weather data found with: ");
        for (String place : places) {
          sb.append(place).append(" ");
        }
      } else {

        int xx = 0;
        List<ForecastResponse> forecastResponses = data.getForecastResponses();

        int longestCityName = findLongestCityNameLength(forecastResponses);

        forecastResponses.sort(
            Comparator.comparing((ForecastResponse response) -> Double.parseDouble(response.current().temp_c()))
                .reversed());
        double highestTemp = Double.parseDouble(forecastResponses.get(0).current().temp_c());

        for (ForecastResponse forecastResponse : forecastResponses) {
          String formatted;
          if (xx != 0) {
            double diff = highestTemp - Double.parseDouble(forecastResponse.current().temp_c());
            String differenceStr = String.format("%.2f°C", diff);
            formatted = formatWeather(forecastResponse, differenceStr, longestCityName);
            sb.append("\n");
          } else {
            formatted = formatWeather(forecastResponse, "difference", longestCityName);
          }
          sb.append(formatted);
          xx++;
        }
      }
      return sb.toString();
    } else {
      return this.getClass().getSimpleName() + " error :: " + data.getStatus();
    }
  }

}
