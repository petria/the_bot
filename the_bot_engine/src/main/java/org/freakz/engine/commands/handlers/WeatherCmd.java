package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.dto.KelikameratResponse;
import org.freakz.dto.KelikameratWeatherData;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_COUNT;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;


@HokanCommandHandler
@Slf4j
public class WeatherCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Get weather for city.");

        FlaggedOption flg = new FlaggedOption(ARG_COUNT)
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("5")
                .setShortFlag('c');
        jsap.registerParameter(flg);

        UnflaggedOption opt = new UnflaggedOption(ARG_PLACE)
                .setDefault("Oulu")
                .setRequired(true)
                .setGreedy(false);

        jsap.registerParameter(opt);

    }

    private String formatWeather(KelikameratWeatherData d, boolean verbose) {
        String template = "%s: %2.1f°C";
        String placeFromUrl = d.getPlaceFromUrl();
        placeFromUrl = placeFromUrl.substring(placeFromUrl.indexOf("_") + 1).replaceAll("_", " ");
        String ret = String.format(template, placeFromUrl, d.getAir(), d.getRoad(), d.getGround());
        if (verbose) {
            ret += " [" + d.getUrl() + "]";
        }
        return ret;
    }

    @Override
    public String executeCommand(EngineRequest engineRequest, JSAPResult results) {

//        boolean verbose = results.getBoolean(ARG_VERBOSE);
        boolean verbose = false;
        String place = results.getString(ARG_PLACE);
        log.debug("Place: {}", place);

        KelikameratResponse data = doServiceRequest(engineRequest, results, ServiceRequestType.KelikameratService);
        if (data.getStatus().startsWith("OK")) {
            StringBuilder sb = new StringBuilder();
            int xx = 0;

            for (KelikameratWeatherData wd : data.getDataList()) {
                String formatted = formatWeather(wd, verbose);

                if (formatted.contains("Rantatunneli")) {
                    continue;
                }

                if (xx != 0) {
                    sb.append(", ");
                }
                sb.append(formatted);
                xx++;
                if (xx >= results.getInt(ARG_COUNT)) {
                    break;
                }
            }
            return sb.toString();
        } else {
            return this.getClass().getSimpleName() + " error :: " + data.getStatus();
        }

    }
}
