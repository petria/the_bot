package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.*;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.common.model.json.foreca.ForecaData;
import org.freakz.dto.ForecaResponse;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.*;


@HokanCommandHandler
@Slf4j
public class ForecaCmd extends AbstractCmd {

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
                .setDefault("Oulu")
                .setRequired(true)
                .setGreedy(false);

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

        String place = results.getString(ARG_PLACE);

        log.debug("Place: {}", place);
        ForecaResponse data = doServiceRequest(engineRequest, results, ServiceRequestType.ForecaWeatherService);
        if (data.getStatus().startsWith("OK")) {
            StringBuilder sb = new StringBuilder();

            if (data.getForecaDataList().size() == 0) {
                sb.append("Check spelling, no Foreca data found with: ");
                sb.append(place);
            } else {
                int xx = 0;
                for (ForecaData forecaData : data.getForecaDataList()) {
                    String formatted = formatWeather(forecaData, verbose, sunUpDown, feelsLike);
                    if (xx != 0) {
                        sb.append(", ");
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
