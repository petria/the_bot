package org.freakz.engine.services.timeservice;

import java.time.LocalDateTime;
import org.freakz.common.model.TimeDifferenceData;

public interface TimeDifferenceService {

  TimeDifferenceData getTimeDifference(LocalDateTime fromDateTime, LocalDateTime toDateTime);

  TimeDifferenceData getTimeDifference(long fromTimestamp, long toTimestamp);
}
