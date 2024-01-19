package org.freakz.engine.services.topcounter;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataValues;
import org.freakz.engine.data.service.DataValuesService;
import org.freakz.engine.dto.TopStatsResponse;
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

    public TopStatsRequestHandlerService(DataValuesService dataValuesService) {
        this.dataValuesService = dataValuesService;
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
        List<DataValues> values = dataValuesService.findAllByNickAndChannelAndNetworkAndKeyNameIsLike(nick, channel, network, "GLUGGA_COUNT_.*");
        mapValues(values, response);
        String res = String.format("%s %s %s %s", key, nick, channel, network);

        response.setStatus("handleTopStatsRequest: " + res);
        return response;
    }


    private void mapValues(List<DataValues> dataValues, TopStatsResponse response) {
        Map<LocalDate, DataValues> mapped1 = new HashMap<>();
        List<LocalDate> dateList = new ArrayList<>();
        for (DataValues model : dataValues) {
            String[] split = model.getKeyName().split("_");
            int year = Integer.parseInt(split[2]);
            int month = Integer.parseInt(split[4]);
            int day = Integer.parseInt(split[3]);
            LocalDate date = LocalDate.of(year, month, day);
            dateList.add(date);
            mapped1.put(date, model);
        }
        Collections.sort(dateList);
        int days = 0;
        int drinkDays = 0;
        if (dateList.size() > 1) {
            LocalDate loopDate = dateList.getFirst();
            LocalDate today = LocalDate.now();
            boolean done = false;
            while (!done) {
                days++;
                DataValues forDay = mapped1.get(loopDate);
                if (forDay != null) {
                    int foo = 0;
                    drinkDays++;
                }
                loopDate = loopDate.plusDays(1);
                if (loopDate.isAfter(today)) {
                    done = true;
                }
            }

        }
        response.setFirstDay(dateList.getFirst());
        response.setTotalDays(days);
        response.setStatDays(drinkDays);
        int foo = 0;

    }

}
// !topstats glugga unh #amigafin ircnet
// !topstats glugga _pete_ #amigafin ircnet
// !topstats glugga shd #amigafin ircnet
// !topstats glugga pyksy #amigafin ircnet
