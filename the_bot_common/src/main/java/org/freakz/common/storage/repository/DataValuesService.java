package org.freakz.common.storage.repository;

import org.freakz.common.storage.DataValueStatsModel;
import org.freakz.common.storage.DataValuesModel;

import java.util.List;

public interface DataValuesService {

    List<DataValuesModel> getDataValues(String channel, String network, String key);

    List<DataValuesModel> getDataValuesAsc(String channel, String network, String key);

    List<DataValuesModel> getDataValuesDesc(String channel, String network, String key);

    String getValue(String nick, String channel, String network, String key);

    void setValue(String nick, String channel, String network, String key, String value);

    DataValueStatsModel getValueStats(String nick, String channel, String network, String key);
}
