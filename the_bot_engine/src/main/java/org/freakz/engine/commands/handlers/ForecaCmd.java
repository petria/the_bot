package org.freakz.engine.commands.handlers;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.common.model.json.foreca.ForecaData;
import org.freakz.dto.ForecaResponse;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_COUNT;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_VERBOSE;


@HokanCommandHandler
@Slf4j
public class ForecaCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Get FORECA weather for city.");

        FlaggedOption flg = new FlaggedOption(ARG_COUNT)
                .setStringParser(JSAP.INTEGER_PARSER)
                .setDefault("5")
                .setLongFlag("count")
                .setShortFlag('c');
        jsap.registerParameter(flg);

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

    private String formatWeather(ForecaData d, boolean verbose) {

        String v = "";
        if (verbose) {
            v = d.getCityLink().region + "/" + d.getCityLink().country + "/";
        }
        String template = "%s%s: %s %2.1f°C (feels like: %2.1f°C)";

        String placeFromUrl = d.getCityLink().city2;

        String ret = String.format(template, v, placeFromUrl, d.getWeatherData().getTime().replaceAll("\\.", ":"), d.getWeatherData().getTemp(), d.getWeatherData().getFeelsLike());
        return ret;
    }

    @Override
    public String executeCommand(EngineRequest engineRequest, JSAPResult results) {


        boolean verbose = results.getBoolean(ARG_VERBOSE);
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
                    String formatted = formatWeather(forecaData, verbose);
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
