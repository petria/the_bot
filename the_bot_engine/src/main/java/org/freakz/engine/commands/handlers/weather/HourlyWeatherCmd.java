package org.freakz.engine.commands.handlers.weather;

import com.martiansoftware.jsap.*;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.weather.WeatherAPIResponse;
import org.freakz.engine.services.api.ServiceRequestType;
import org.freakz.engine.services.weather.weatherapi.model.Astro;
import org.freakz.engine.services.weather.weatherapi.model.AstronomyResponse;
import org.freakz.engine.services.weather.weatherapi.model.ForecastDay;
import org.freakz.engine.services.weather.weatherapi.model.ForecastResponse;

import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_ASTRONOMY;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

@HokanCommandHandler
@Slf4j
public class HourlyWeatherCmd extends AbstractCmd {


    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Query weather using weatherapi.com services. Show hourly forecast");

        Switch astronomy = new Switch(ARG_ASTRONOMY)
                .setLongFlag("astronomy")
                .setShortFlag('a');
        jsap.registerParameter(astronomy);

        UnflaggedOption opt = new UnflaggedOption(ARG_PLACE)
                .setDefault("Oulu")
                .setRequired(true)
                .setGreedy(false);
        jsap.registerParameter(opt);

    }

    @Override
    public List<HandlerAlias> getAliases(String botName) {
        List<HandlerAlias> list = new ArrayList<>();
        list.add(createWithArgsAlias("!hsaa", "!hourlyweather"));
/*        list.add(createWithArgsAlias("!saa", "!weather"));
        list.add(createWithArgsAlias("!sää", "!weather"));
        list.add(createWithArgsAlias("!foreca", "!weather"));
        list.add(createWithArgsAlias("!keli", "!weather"));*/

        return list;
    }

    @Override
    public String executeCommand(EngineRequest engineRequest, JSAPResult results) {
        String city = results.getString(ARG_PLACE);

        WeatherAPIResponse response = doServiceRequestMethods(engineRequest, results, ServiceRequestType.WeatherAPIService);
        if (response.getStatus().startsWith("OK")) {
            ForecastResponse r = response.getForecastResponseModel();
            StringBuilder hours = new StringBuilder();
            StringBuilder temps = new StringBuilder();
            int[] hoursToShow = {0, 3, 6, 9, 12, 14, 16, 19, 20, 22, 23};
            for (int hh : hoursToShow) {
                hours.append(String.format("%5d", hh));
            }
            for (ForecastDay forecastDay : r.forecast().forecastday()) {
//                if (!temps.isEmpty()) {
//                }
                for (int hh : hoursToShow) {
                    temps.append(String.format("%5s", forecastDay.hour().get(hh).temp_c()));
                }
                temps.append(" - ");
                temps.append(forecastDay.date());
                temps.append("\n");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Hourly forecast: ");
            sb.append(r.location().name());
            sb.append("\n");

            sb.append(hours);
            sb.append("\n");
            sb.append(temps);
            sb.append("\n");

            return sb.toString();

        } else {
            return String.format("%s: %s", results.getString(ARG_PLACE), response.getErrorResponse().error().message());
        }
    }

    private String formatTime(ForecastResponse r) {
        return String.format("%s", r.current().last_updated().split(" ")[1]);
    }

    private String formatAstronomy(AstronomyResponse r, boolean doAstronomy) {
        if (doAstronomy && r != null) {
            Astro a = r.astronomy().astro();
            return String.format(" - sun up: %s / down %s -- moon: %s", a.sunrise(), a.sunset(), a.moon_phase());
        }
        return "";
    }

    private String formatFeelsLike(ForecastResponse r, boolean doFeelsLike) {
        if (doFeelsLike) {
            return String.format(" (feels like %s°C)", r.current().feelslike_c());
        }
        return "";
    }

    private String formatName(ForecastResponse r, boolean verbose) {
        if (verbose) {
            return String.format("%s/%s/%s", r.location().name(), r.location().region(), r.location().country());
        }
        return String.format("%s", r.location().name());
    }
}
