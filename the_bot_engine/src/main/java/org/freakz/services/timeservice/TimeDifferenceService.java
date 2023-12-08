package org.freakz.services.timeservice;


import org.freakz.common.model.TimeDifferenceData;

import java.time.LocalDateTime;

public interface TimeDifferenceService {

    TimeDifferenceData getTimeDifference(LocalDateTime fromDateTime, LocalDateTime toDateTime);

    TimeDifferenceData getTimeDifference(long fromTimestamp, long toTimestamp);
}
