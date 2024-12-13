package org.freakz.engine.data.repository;

public interface DataSavingService {

  void checkIsSavingNeeded();

  DataSaverInfo getDataSaverInfo();
}
