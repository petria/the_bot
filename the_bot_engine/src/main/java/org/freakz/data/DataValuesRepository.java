package org.freakz.data;

import org.freakz.common.storage.DataValues;
import org.freakz.config.ConfigService;

import java.util.List;

public interface DataValuesRepository {
    List<DataValues> findAllByNickAndChannelAndNetworkAndKeyNameIsLike(String nick, String channel, String network, String keyLike);

    List<DataValues> findAllByChannelAndNetworkAndKeyNameIsLike(String channel, String network, String keyLike);

    DataValues findByNickAndChannelAndNetworkAndKeyName(String nick, String channel, String network, String key);

    DataValues save(DataValues data);

    void initialize(ConfigService configService) throws Exception;
}
