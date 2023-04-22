package org.freakz.data.repository;

import org.freakz.common.data.dto.DataBase;
import org.freakz.common.exception.DataRepositoryException;
import org.freakz.common.data.dto.DataValues;

import java.util.List;

public interface DataValuesRepository extends DataBaseRepository<DataValues> {
    List<DataValues> findAllByNickAndChannelAndNetworkAndKeyNameIsLike(String nick, String channel, String network, String keyLike);

    List<DataValues> findAllByChannelAndNetworkAndKeyNameIsLike(String channel, String network, String keyLike);

    DataValues findByNickAndChannelAndNetworkAndKeyName(String nick, String channel, String network, String key);

    DataValues save(DataValues data) throws DataRepositoryException;

}
