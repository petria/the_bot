package org.freakz.data.repository.impl;

import org.freakz.common.exception.DataRepositoryException;
import org.freakz.common.model.dto.DataValues;
import org.freakz.data.repository.DataBaseRepository;

import java.util.List;

public interface DataValuesRepository extends DataBaseRepository<DataValues> {
    List<DataValues> findAllByNickAndChannelAndNetworkAndKeyNameIsLike(String nick, String channel, String network, String keyLike);

    List<DataValues> findAllByChannelAndNetworkAndKeyNameIsLike(String channel, String network, String keyLike);

    DataValues findByNickAndChannelAndNetworkAndKeyName(String nick, String channel, String network, String key);

    DataValues save(DataValues data) throws DataRepositoryException;

}
