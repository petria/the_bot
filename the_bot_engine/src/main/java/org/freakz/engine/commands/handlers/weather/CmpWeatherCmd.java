package org.freakz.engine.commands.handlers.weather;

import com.martiansoftware.jsap.*;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.foreca.ForecaData;
import org.freakz.dto.CmpWeatherResponse;
import org.freakz.dto.ForecaResponse;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.*;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_COUNT;

@HokanCommandHandler
@Slf4j
public class CmpWeatherCmd extends AbstractCmd {
    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Get Foreca weather data for city. See https://www.foreca.fi/haku for city names!");

        FlaggedOption flg = new FlaggedOption(ARG_COUNT)
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("5")
                .setLongFlag("count")
                .setShortFlag('c');
        jsap.registerParameter(flg);

        Switch feelsLike = new Switch(ARG_FEELS_LIKE)
                .setLongFlag("feelsLike")
                .setShortFlag('f');
        jsap.registerParameter(feelsLike);

        Switch sunUpDown = new Switch(ARG_SUN_UP_DOWN)
                .setLongFlag("sunUpDown")
                .setShortFlag('s');
        jsap.registerParameter(sunUpDown);

        Switch verbose = new Switch(ARG_VERBOSE)
                .setLongFlag("verbose")
                .setShortFlag('v');
        jsap.registerParameter(verbose);


        UnflaggedOption opt = new UnflaggedOption(ARG_PLACE)
                .setList(true)
                .setDefault(new String[]{"Oulu", "Jaipur"})
                .setRequired(true)
                .setGreedy(true);

        jsap.registerParameter(opt);

    }

    private String formatWeather(ForecaData d, boolean verbose, boolean sunUpDown, boolean feelsLike) {

        String v = "";
        if (verbose) {
            v = d.getCityLink().region + "/" + d.getCityLink().country + "/";
        }
        String upDown = "";
        if (sunUpDown) {
            upDown
                    = String.format(" - Sun up/down: %s - %s (%dh %dm)",
                    d.getSunUpDown().getSunUpTime(),
                    d.getSunUpDown().getSunDownTime(),
                    d.getSunUpDown().getDayLengthHours(),
                    d.getSunUpDown().getDayLengthMinutes()
            );
        }
        String feels = "";
        if (feelsLike) {
            feels = String.format(" (feels like: %2.1f°C)", d.getWeatherData().getFeelsLike());
        }
        String template = "%s%s: %s %s %2.1f°C%s%s";

        return String.format(template, v, d.getCityLink().city2, d.getWeatherData().getDate(), d.getWeatherData().getTime().replaceAll("\\.", ":"), d.getWeatherData().getTemp(), feels, upDown);
    }

    @Override
    public String executeCommand(EngineRequest engineRequest, JSAPResult results) {


        boolean verbose = results.getBoolean(ARG_VERBOSE);
        boolean sunUpDown = results.getBoolean(ARG_SUN_UP_DOWN);
        boolean feelsLike = results.getBoolean(ARG_FEELS_LIKE);

        String p = results.getString(ARG_PLACE);

        String[] places = results.getStringArray(ARG_PLACE);

//        log.debug("place: {}",p);
        for (String place : places) {
            log.debug("place: {}", place);
        }
        CmpWeatherResponse data = doServiceRequest(engineRequest, results, ServiceRequestType.CmpWeatherService);
        if (data.getStatus().startsWith("OK")) {
            StringBuilder sb = new StringBuilder();

            if (data.getForecaDataList().size() == 0) {
                sb.append("Check spelling, no Foreca data found with: ");
                for (String place : places) {
                    sb.append(place).append(" ");
                }
            } else {
                int xx = 0;
                for (ForecaData forecaData : data.getForecaDataList()) {
                    String formatted = formatWeather(forecaData, verbose, sunUpDown, feelsLike);
                    if (xx != 0) {
                        sb.append("\n");
                    }
                    sb.append(formatted);
                    xx++;
                    if (xx >= results.getInt(ARG_COUNT)) {
                        break;
                    }
                }
            }
            return sb.toString();
        } else {
            return this.getClass().getSimpleName() + " error :: " + data.getStatus();
        }

    }
}
