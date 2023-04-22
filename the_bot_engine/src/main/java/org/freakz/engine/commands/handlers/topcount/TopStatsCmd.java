package org.freakz.engine.commands.handlers.topcount;

import com.martiansoftware.jsap.*;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.enums.TopCountsEnum;
import org.freakz.common.model.json.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;

import static org.freakz.engine.commands.util.StaticArgumentStrings.*;


@HokanCommandHandler
@Slf4j
public class TopStatsCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Stats about counted thing, TopKey parameter need to one of: " + String.join(", ", TopCountsEnum.getPrettyNames()));

        FlaggedOption flg = new FlaggedOption(ARG_COUNT);
        UnflaggedOption uflg = new UnflaggedOption(ARG_TOP_KEY)
                .setRequired(false)
                .setGreedy(false);
        jsap.registerParameter(uflg);

        UnflaggedOption opt = new UnflaggedOption(ARG_NICK)
                .setDefault("me")
                .setRequired(false)
                .setGreedy(false);
        jsap.registerParameter(opt);
    }

    private String mustBeError() {
        return String.format("%s\n  TopKey must be one of: %s", getJsap().getUsage(), String.join(", ", TopCountsEnum.getPrettyNames()));
    }

    @Override
    public String executeCommand(EngineRequest engineRequest, JSAPResult results) {
        String topKey = results.getString(ARG_TOP_KEY);
        if (topKey == null) {
            return mustBeError();
        }
        TopCountsEnum countsEnum = TopCountsEnum.getByPrettyName(topKey);
        if (countsEnum == null) {
            return mustBeError();
        }


        String channel = engineRequest.getReplyTo().toLowerCase();
        String network = engineRequest.getNetwork().toLowerCase();


        String nick;
        if (results.getString(ARG_NICK).equals("me")) {
            nick = engineRequest.getFromSender().toLowerCase();
        } else {
            nick = results.getString(ARG_NICK).toLowerCase();
        }
        // TODO
        return "N/A";
    }
}
