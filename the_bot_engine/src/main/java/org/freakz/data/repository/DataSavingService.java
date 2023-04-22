package org.freakz.data.repository;

public interface DataSavingService {

    void checkIsSavingNeeded();

    DataSaverInfo getDataSaverInfo();

}
