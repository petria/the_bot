package org.freakz.data;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.exception.DataRepositoryException;
import org.freakz.common.storage.DataValueStatsModel;
import org.freakz.common.storage.DataValues;
import org.freakz.common.storage.DataValuesModel;
import org.freakz.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@Slf4j
public class DataValuesServiceImpl implements DataValuesService {

    //    @Autowired
    private final DataValuesRepository dataValuesRepository;

    private final ConfigService configService;

    @Autowired
    public DataValuesServiceImpl(ConfigService configService) throws Exception {
        this.configService = configService;
        this.dataValuesRepository = new DataValuesRepositoryImpl(configService);
    }


    private Map<String, DataValuesModel> combineCounters(List<DataValues> modelsList, String key) {
        Map<String, DataValuesModel> combinedMap = new HashMap<>();

        for (DataValues v : modelsList) {
            String mapKey = String.format("%s_%s_%s", v.getNick(), v.getNetwork(), v.getChannel());

            DataValuesModel n = combinedMap.get(mapKey);
            if (n == null) {
                n = new DataValuesModel(v.getNick(), v.getNetwork(), v.getChannel(), key, "1");
                n.setNumberValue(1);
                combinedMap.put(mapKey, n);
            } else {
                n.addToNumberValue(Integer.parseInt(v.getValue()));
                n.setValue(String.valueOf(n.getNumberValue()));
            }
        }
        return combinedMap;
    }

    @Override
//    @Transactional(readOnly = true)
    public DataValueStatsModel getValueStats(String nick, String channel, String network, String key) {
        DataValueStatsModel stats = new DataValueStatsModel();
        String[] days = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};

        String keyLike = key + ".*";
        List<DataValues> modelsList = dataValuesRepository.findAllByNickAndChannelAndNetworkAndKeyNameIsLike(nick, channel, network, keyLike);
        Map<String, Integer> dayCounts = new HashMap<>();
        for (String day : days) {
            dayCounts.put(day, 0);
        }
        Map<Integer, Integer> hourCounts = new HashMap<>();
        IntStream.rangeClosed(0, 23).forEach(
                i -> hourCounts.put(i, 0)
        );
        for (String day : days) {
            dayCounts.put(day, 0);
        }

        int totalCount = 0;
        for (DataValues dv : modelsList) {
            LocalDateTime ldt = parseTimeFromKey(dv.getKeyName());
            String day = days[ldt.getDayOfWeek().getValue() - 1];
            int val = Integer.parseInt(dv.getValue());

            int dayCount = dayCounts.get(day);
            dayCount += val;
            dayCounts.put(day, dayCount);

            totalCount += val;

            int hourVal = hourCounts.get(ldt.getHour());
            hourVal += val;
            hourCounts.put(ldt.getHour(), hourVal);
        }

        Map<String, Double> dayCountPercent = new HashMap<>();
        for (String day : days) {
            double dayPercent = (dayCounts.get(day) * 100D) / totalCount;
            dayCountPercent.put(day, dayPercent);
        }

        String dayRet = "";
        for (String day : days) {
            dayRet += String.format("%s: %02.2f%% ", day, dayCountPercent.get(day));
        }
        stats.setOutput(dayRet + " = " + totalCount);


        final String[] hourRows = {"", ""};

        IntStream.rangeClosed(0, 23).forEach(
                i -> {
                    hourRows[0] += String.format("%3d ", i);
                    hourRows[1] += String.format("%3d ", hourCounts.get(i));
                }
        );
        String hoursRet = "\n" + hourRows[0] + "\n" + hourRows[1] + "\n";
        stats.setOutput(hoursRet);

        return stats;
    }

    private LocalDateTime parseTimeFromKey(String keyName) {
        //GLUGGA_COUNT_2021_27_03_16
        String[] split = keyName.split("_");
        LocalDateTime time = LocalDateTime.now()
                .withYear(Integer.parseInt(split[2]))
                .withDayOfMonth(Integer.parseInt(split[3]))
                .withMonth(Integer.parseInt(split[4]))
                .withHour(Integer.parseInt(split[5]))
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        return time;
    }

    @Override
//    @Transactional(readOnly = true)
    public List<DataValuesModel> getDataValues(String channel, String network, String key) {
        String keyLike = key + "%";
        List<DataValues> modelsList = dataValuesRepository.findAllByChannelAndNetworkAndKeyNameIsLike(channel, network, keyLike);

        final Map<String, DataValuesModel> combinedMap = combineCounters(modelsList, key);
        List<DataValuesModel> models = new ArrayList<>(combinedMap.values());
        return models;
    }

    @Override
//    @Transactional(readOnly = true)
    public List<DataValuesModel> getDataValuesAsc(String channel, String network, String key) {
        String keyLike = key + ".*";
        List<DataValues> modelsList = dataValuesRepository.findAllByChannelAndNetworkAndKeyNameIsLike(channel, network, keyLike);

        final Map<String, DataValuesModel> combinedMap = combineCounters(modelsList, key);
        List<DataValuesModel> dataValues = new ArrayList<>(combinedMap.values());

        Comparator<? super DataValuesModel> comparator = (Comparator<DataValuesModel>) (o1, o2) -> {
            Integer i1 = Integer.parseInt(o1.getValue());
            Integer i2 = Integer.parseInt(o2.getValue());
            return i2.compareTo(i1);
        };
        dataValues.sort(comparator);
        return dataValues;
    }

    @Override
//    @Transactional(readOnly = true)
    public List<DataValuesModel> getDataValuesDesc(String channel, String network, String key) {
        String keyLike = key + "%";
        List<DataValues> modelsList = dataValuesRepository.findAllByChannelAndNetworkAndKeyNameIsLike(channel, network, keyLike);

        final Map<String, DataValuesModel> combinedMap = combineCounters(modelsList, key);
        List<DataValuesModel> dataValues = new ArrayList<>(combinedMap.values());

        Comparator<? super DataValuesModel> comparator = (Comparator<DataValuesModel>) (o1, o2) -> {
            Integer i1 = Integer.parseInt(o1.getValue());
            Integer i2 = Integer.parseInt(o2.getValue());
            return i1.compareTo(i2);
        };
        dataValues.sort(comparator);
        return dataValues;
    }

    @Override
//    @Transactional(readOnly = true)
    public String getValue(String nick, String channel, String network, String key) {
        String value = null;
        DataValues data = dataValuesRepository.findByNickAndChannelAndNetworkAndKeyName(nick, channel, network, key);
        if (data != null) {
            value = data.getValue();
        }
        return value;
    }

    @Override
//    @Transactional
    public void setValue(String nick, String channel, String network, String key, String value) throws DataRepositoryException {
        DataValues data = dataValuesRepository.findByNickAndChannelAndNetworkAndKeyName(nick, channel, network, key);
        if (data == null) {
            data = new DataValues();
            data.setNick(nick);
            data.setChannel(channel);
            data.setNetwork(network);
            data.setKeyName(key);
        }
        data.setValue(value);
        data = dataValuesRepository.save(data);

    }


    @Scheduled(fixedRate = 100)
    public void repositorySaveTimer() {
        this.dataValuesRepository.checkIsSavingNeeded();
    }

}
