package org.freakz.engine.commands.handlers.topcount;

import com.martiansoftware.jsap.*;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.enums.TopCountsEnum;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.stats.StatsNode;
import org.freakz.engine.dto.stats.TopStatsResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.*;


@HokanCommandHandler
@Slf4j
public class TopStatsCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Stats about counted thing, TopKey parameter need to one of: " + String.join(", ", TopCountsEnum.getPrettyNames()));

        FlaggedOption flg = new FlaggedOption(ARG_COUNT);

        UnflaggedOption opt = new UnflaggedOption(ARG_NICK)
                .setDefault("me")
                .setRequired(false)
                .setGreedy(false);
        jsap.registerParameter(opt);

        UnflaggedOption uflg = new UnflaggedOption(ARG_TOP_KEY)
                .setDefault("glugga")
                .setRequired(false)
                .setGreedy(false);
        jsap.registerParameter(uflg);

        opt = new UnflaggedOption(ARG_CHANNEL)
                .setDefault("current")
                .setRequired(false)
                .setGreedy(false);
        jsap.registerParameter(opt);

        opt = new UnflaggedOption(ARG_NETWORK)
                .setDefault("current")
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



        String nick;
        if (results.getString(ARG_NICK).equals("me")) {
            nick = engineRequest.getFromSender().toLowerCase();
        } else {
            nick = results.getString(ARG_NICK).toLowerCase();
        }

        TopStatsResponse response = doServiceRequestMethods(engineRequest, results, ServiceRequestType.GetTopStatsRequest);


        StatsNode statsNode = response.getNodeMap().get(nick.toLowerCase());
        //        List<StatsNode> collect = nodeMap.values().stream().sorted(Comparator.comparing(o -> o.statDaysPercent, Comparator.reverseOrder())).toList();
        if (statsNode != null) {
            double statDays = statsNode.statsDays;
            double totalDays = statsNode.totalDays;
            double percent = statDays / totalDays * 100D;
            // TODO
            String stats = String.format("%s :: %s stats %d/%d: %f %% - longest %s spree: %d - longest NO %s spree: %d days", statsNode.nick, topKey, statsNode.statsDays, statsNode.totalDays, percent, topKey, statsNode.plusStreakDays, topKey, statsNode.minusStreakDays);
            return stats;

        } else {
            return String.format("No stats found with: %s", nick);
        }

    }
}
