package org.freakz.engine.commands.handlers.topcount;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.enums.TopCountsEnum;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.common.data.dto.DataValuesModel;
import org.freakz.dto.TopCountsResponse;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.ServiceRequestType;

import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_CHANNEL;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_TOP_KEY;


@HokanCommandHandler
@Slf4j
public class TopCountsCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Channel top key word counts. TopKey parameter need to one of: " + String.join(", ", TopCountsEnum.getPrettyNames()));

        UnflaggedOption uflg = new UnflaggedOption(ARG_TOP_KEY)
                .setRequired(false)
                .setGreedy(false);
        jsap.registerParameter(uflg);

        uflg = new UnflaggedOption(ARG_CHANNEL)
                .setRequired(false)
                .setGreedy(false);
        jsap.registerParameter(uflg);
    }

    @Override
    public List<HandlerAlias> getAliases() {
        List<HandlerAlias> list = new ArrayList<>();
        list.add(createAlias("!topgl", "!topcounts GLUGGA_COUNT"));
        list.add(createAlias("!topkor", "!topcounts KORINA_COUNT"));
        list.add(createAlias("!topryyst", "!topcounts RYYST_COUNT"));
        return list;
    }

    private String formatKeys() {
        return String.join(", ", TopCountsEnum.getPrettyNames());
    }

    @Override
    public String executeCommand(EngineRequest engineRequest, JSAPResult results) {
        String key = results.getString(ARG_TOP_KEY);
        if (key == null) {
            String ret = String.format("%s\n  TopKey must be one of: %s", getJsap().getUsage(), formatKeys());
            return ret;
        }

        String channel;
        if (engineRequest.isPrivateChannel()) {
            channel = "#amigafin";
        } else {
            channel = results.getString(ARG_CHANNEL, engineRequest.getReplyTo()).toLowerCase();
        }


        TopCountsResponse response = doServiceRequest(engineRequest, results, ServiceRequestType.GetTopCountsService);
        List<DataValuesModel> dataValues = response.getDataValues();
        if (dataValues.size() > 0) {
            int c = 1;
            String starredKey = String.format(" *%s*: ", dataValues.get(0).getKeyName().split("_")[0].toLowerCase());
            StringBuilder sb = new StringBuilder("Top " + channel + starredKey);
            for (DataValuesModel value : dataValues) {
                sb.append(c).append(") ");
                sb.append(value.getNick());
                sb.append("=");
                sb.append(value.getValue());
                sb.append(" ");
                c++;
                if (c == 11) {
                    sb.append("(").append(dataValues.size()).append(" total in list!)");
                    break;
                }
            }
            return sb.toString();
        }
        return String.format("TopCounts with %s: none yet!", key);
    }
}
