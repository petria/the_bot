package org.freakz.engine.data.service;

import org.freakz.common.exception.DataRepositoryException;
import org.freakz.common.model.dto.DataValueStatsModel;
import org.freakz.common.model.dto.DataValuesModel;

import java.util.List;

public interface DataValuesService {

  List<DataValuesModel> getDataValues(String channel, String network, String key);

  List<DataValuesModel> getDataValuesAsc(String channel, String network, String key);

  List<DataValuesModel> getDataValuesDesc(String channel, String network, String key);

  String getValue(String nick, String channel, String network, String key);

  void setValue(String nick, String channel, String network, String key, String value)
      throws DataRepositoryException;

  DataValueStatsModel getValueStats(String nick, String channel, String network, String key);

  //    List<DataValues> findAllByNickAndChannelAndNetworkAndKeyNameIsLike(String nick, String
  // channel, String network, String keyLike);
}
