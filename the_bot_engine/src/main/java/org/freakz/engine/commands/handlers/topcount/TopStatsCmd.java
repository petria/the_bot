package org.freakz.engine.commands.handlers.topcount;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.UnflaggedOption;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.enums.TopCountsEnum;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.engine.commands.HandlerAlias;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.engine.dto.stats.StatsNode;
import org.freakz.engine.dto.stats.TopStatsResponse;
import org.freakz.engine.services.api.ServiceRequestType;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.*;


@HokanCommandHandler
@Slf4j
public class TopStatsCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {

        jsap.setHelp("Stats about counted thing, TopKey parameter need to one of: " + String.join(", ", TopCountsEnum.getPrettyNames()));


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

    @Override
    public List<HandlerAlias> getAliases(String botName) {
        List<HandlerAlias> list = new ArrayList<>();
        list.add(createAlias("!myglugga", "!topstats me glugga"));
        return list;
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

            DateTimeFormatter pattern = DateTimeFormatter.ofPattern("dd.MM.yyyy");


            double statDays = statsNode.statsDays;
            double totalDays = statsNode.totalDays;
            double percent = statDays / totalDays * 100D;


            String firstStatDay = statsNode.firstStatDay.format(pattern);
            String lastStatDay = statsNode.lastStatDay.format(pattern);

            String plusStreakStartDay = statsNode.plusStreakStart.format(pattern);
            String plusStreakEndDay = statsNode.plusStreakEnd.format(pattern);

            String minusStreakStartDay = statsNode.minusStreakStart.format(pattern);
            String minusStreakEndDay = statsNode.minusStreakEnd.format(pattern);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("==== %s stats: %s", topKey, engineRequest.getFromSender()));
            sb.append(String.format(" (%s - %s)\n", firstStatDay, lastStatDay));

            sb.append(String.format("       plus  streak: %s - %s = %d days\n", plusStreakStartDay, plusStreakEndDay, statsNode.plusStreakDays));
            sb.append(String.format("       minus streak: %s - %s = %d days\n", minusStreakStartDay, minusStreakEndDay, statsNode.minusStreakDays));
            sb.append(String.format(" days used/not used: %d - %d = %f %%\n", statsNode.statsDays, statsNode.totalDays, percent));

//            String streakStart = String.format("%02d.%02d.%4d", statsNode.plusStreakStart.getDayOfMonth(), statsNode.firstStatDay.getMonthValue(), statsNode.firstStatDay.getYear());
//            String stats = String.format("%s :: %s stats starting from %s days used/not used %d/%d ratio: %f %% - longest %s spree: %d - longest NO %s spree: %d days", engineRequest.getFromSender(), topKey, firstStatDay, statsNode.statsDays, statsNode.totalDays, percent, topKey, statsNode.plusStreakDays, topKey, statsNode.minusStreakDays);
            return sb.toString();

        } else {
            return String.format("No stats found with: %s", nick);
        }

    }
}
