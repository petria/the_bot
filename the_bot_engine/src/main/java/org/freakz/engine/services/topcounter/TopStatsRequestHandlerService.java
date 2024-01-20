package org.freakz.engine.services.topcounter;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataValues;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.repository.impl.DataValuesRepository;
import org.freakz.engine.data.repository.impl.DataValuesRepositoryImpl;
import org.freakz.engine.data.service.DataValuesService;
import org.freakz.engine.dto.stats.StatsNode;
import org.freakz.engine.dto.stats.TopStatsResponse;
import org.freakz.engine.services.api.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

import static org.freakz.engine.commands.util.StaticArgumentStrings.*;

@Service
@SpringServiceMethodHandler
@Slf4j
public class TopStatsRequestHandlerService extends AbstractSpringService {

    private final DataValuesService dataValuesService;

    private final ConfigService configService;
    private final DataValuesRepository dataValuesRepository;


    public TopStatsRequestHandlerService(DataValuesService dataValuesService, ConfigService configService) throws Exception {
        this.dataValuesService = dataValuesService;
        this.configService = configService;
        this.dataValuesRepository = new DataValuesRepositoryImpl(configService);
    }

    @ServiceMessageHandlerMethod(ServiceRequestType = ServiceRequestType.GetTopStatsRequest)
    public <T extends ServiceResponse> TopStatsResponse handleTopStatsRequest(ServiceRequest request) {

        String channel;
        if (request.getEngineRequest().isPrivateChannel()) {
            channel = "#amigafin";
        } else {
            channel = request.getResults().getString(ARG_CHANNEL, request.getEngineRequest().getReplyTo()).toLowerCase();
        }

        String network;
        if (request.getResults().getString(ARG_NETWORK).equals("current")) {
            network = request.getEngineRequest().getNetwork().toLowerCase();
        } else {
            network = request.getResults().getString(ARG_NETWORK);
        }

        String nick;
        if (request.getResults().getString(ARG_NICK).equals("me")) {
            nick = request.getEngineRequest().getFromSender().toLowerCase();
        } else {
            nick = request.getResults().getString(ARG_NICK).toLowerCase();
        }
        TopStatsResponse response = TopStatsResponse.builder().build();

        String key = request.getResults().getString(ARG_TOP_KEY);
        List<DataValues> allGlugga = dataValuesRepository.findAllByChannelAndNetworkAndKeyNameIsLike(channel, network, "GLUGGA_COUNT_.*");
        Set<String> names = getUniqueNicks(allGlugga);

        response.setNodeMap(mapValues(allGlugga, response, names));

        String res = String.format("%s %s %s %s", key, nick, channel, network);
        response.setStatus("handleTopStatsRequest: " + res);
        return response;
    }

    private Set<String> getUniqueNicks(List<DataValues> allGlugga) {
        Set<String> names = new HashSet<>();
        allGlugga.forEach(dv -> names.add(dv.getNick().toLowerCase()));
        return names;
    }


    private Map<String, StatsNode> mapValues(List<DataValues> dataValues, TopStatsResponse response, Set<String> names) {
        Map<String, StatsNode> nodeMap = new HashMap<>();
        for (String name : names) {

            if (name.equalsIgnoreCase("pyksy")) {
                int foo = 0;
            }

            Map<LocalDate, DataValues> mapped1 = new HashMap<>();
            List<LocalDate> dateList = new ArrayList<>();
            for (DataValues model : dataValues) {
                if (model.getNick().equalsIgnoreCase(name)) {

                    String[] split = model.getKeyName().split("_");
                    int year = Integer.parseInt(split[2]);
                    int day = Integer.parseInt(split[3]);
                    int month = Integer.parseInt(split[4]);
                    LocalDate date = LocalDate.of(year, month, day);
                    dateList.add(date);
                    mapped1.put(date, model);
                }
            }
            Collections.sort(dateList);

            int days = 0;
            int drinkDays = 0;
            int notDrinkDays = 0;

            int inPlusStreakStart = -1;
            int inPlusStreakDays = 0;
            int inPlusHighestDays = -1;
            int inPlusStreakEnd = -1;
            LocalDate plusStreakStart = null;
            LocalDate plusStreakEnd = null;

            int inMinusStreakStart = -1;
            int inMinusStreakDays = 0;
            int inMinusHighestDays = -1;
            int inMinusStreakEnd = -1;
            LocalDate minusStreakStart = null;
            LocalDate minusStreakEnd = null;

            int idx = 0;
            if (dateList.size() > 1) {
                LocalDate loopDate = dateList.getFirst();
                LocalDate today = LocalDate.now();
                boolean done = false;

                LocalDate plusStreakStartTmp = null;
                LocalDate plusStreakEndTmp = null;

                LocalDate minusStreakStartTmp = null;
                LocalDate minusStreakEndTmp = null;

                while (!done) {
                    days++;
                    DataValues forDay = mapped1.get(loopDate);
                    if (forDay != null) {
                        drinkDays++;
                        if (inPlusStreakStart == -1) {
                            inPlusStreakStart = idx;
                            plusStreakStartTmp = loopDate;
                        }

                        if (inMinusStreakStart != -1) {
                            inMinusStreakEnd = idx;
                            minusStreakEndTmp = loopDate;
                        }

                    } else {
                        notDrinkDays++;
                        if (inPlusStreakStart != -1) {
                            inPlusStreakEnd = idx;
                            plusStreakEndTmp = loopDate;
                        }

                        if (inMinusStreakStart == -1) {
                            inMinusStreakStart = idx;
                            minusStreakStartTmp = loopDate;
                        }
                    }

                    if (inMinusStreakStart != -1 && inMinusStreakEnd != -1) {
                        inMinusStreakDays = inMinusStreakEnd - inMinusStreakStart + 1;
                        if (inMinusStreakDays > inMinusHighestDays) {
                            inMinusHighestDays = inMinusStreakDays;
                            minusStreakStart = minusStreakStartTmp;
                            minusStreakEnd = minusStreakEndTmp;
                        }
                        inMinusStreakStart = -1;
                        inMinusStreakEnd = -1;
                    }

                    if (inPlusStreakStart != -1 && inPlusStreakEnd != -1) {
                        inPlusStreakDays = inPlusStreakEnd - inPlusStreakStart + 1;
                        if (inPlusStreakDays > inPlusHighestDays) {
                            inPlusHighestDays = inPlusStreakDays;
                            plusStreakStart = plusStreakStartTmp;
                            plusStreakEnd = plusStreakEndTmp;
                        }
                        inPlusStreakStart = -1;
                        inPlusStreakEnd = -1;
                    }


                    loopDate = loopDate.plusDays(1);
                    idx++;
                    if (loopDate.isAfter(today)) {
                        done = true;
                    }
                }
                StatsNode node = new StatsNode();
                node.nick = name;
                node.totalDays = days;
                node.statsDays = drinkDays;
                node.notStatsDays = notDrinkDays;

                node.statDaysPercent = calcPercent(days, drinkDays);
                node.plusStreakStart = plusStreakStart;
                node.plusStreakEnd = plusStreakEnd;
                node.plusStreakDays = inPlusHighestDays;

                node.minusStreakStart = minusStreakStart;
                node.minusStreakEnd = minusStreakEnd;
                node.minusStreakDays = inMinusHighestDays;

                nodeMap.put(name, node);
            }
        }
        return nodeMap;
    }

    private double calcPercent(double days, double drinkDays) {
        if (days > 0) {
            double percent = drinkDays / days * 100.0D;
            return percent;
        }
        return 0.0D;
    }

}
// !topstats glugga unh #amigafin ircnet
// !topstats glugga _pete_ #amigafin ircnet
// !topstats glugga shd #amigafin ircnet
// !topstats glugga pyksy #amigafin ircnet
