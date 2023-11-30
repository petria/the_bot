package org.freakz.engine.commands.handlers.weather;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.model.foreca.ForecaData;
import org.freakz.dto.CmpWeatherResponse;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.api.ServiceRequestType;

import java.util.Comparator;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_PLACE;

@HokanCommandHandler
@Slf4j
public class CmpWeatherCmd extends AbstractCmd {
    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Compares weather between cities.");

        UnflaggedOption opt = new UnflaggedOption(ARG_PLACE)
                .setList(true)
                .setRequired(true)
                .setGreedy(true);

        jsap.registerParameter(opt);

    }

    private String formatWeather(ForecaData d, String diff, int longestCityName) {

        String template = "%-" + longestCityName + "s %s %s%6.1f°C - %s";
        return String.format(template, d.getCityLink().city2, d.getWeatherData().getDate(), d.getWeatherData().getTime().replaceAll("\\.", ":"), d.getWeatherData().getTemp(), diff);
    }

    private int findLongestCityNameLength(List<ForecaData> forecaDataList) {
        int longest = Integer.MIN_VALUE;
        for (ForecaData data : forecaDataList) {
            if (data.getCityLink().getCity2().length() > longest) {
                longest = data.getCityLink().getCity2().length();
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

            if (data.getForecaDataList().isEmpty()) {
                sb.append("Check spelling, no Foreca data found with: ");
                for (String place : places) {
                    sb.append(place).append(" ");
                }
            } else {

                int xx = 0;
                List<ForecaData> forecaDataList = data.getForecaDataList();

                int longestCityName = findLongestCityNameLength(forecaDataList);

                forecaDataList.sort(Comparator.comparing(l -> ((ForecaData) (l)).getWeatherData().getTemp()).reversed());
                double highestTemp = forecaDataList.get(0).getWeatherData().getTemp();

                for (ForecaData forecaData : forecaDataList) {
                    String formatted;
                    if (xx != 0) {
                        double diff = highestTemp - forecaData.getWeatherData().getTemp();
                        String differenceStr = String.format("%.2f°C", diff);
                        formatted = formatWeather(forecaData, differenceStr, longestCityName);
                        sb.append("\n");
                    } else {
                        formatted = formatWeather(forecaData, "difference", longestCityName);
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
