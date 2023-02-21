package org.freakz.services.timeservice;


import org.freakz.common.model.json.TimeDifferenceData;

import java.time.LocalDateTime;

public interface TimeDifferenceService {

    TimeDifferenceData getTimeDifference(LocalDateTime fromDateTime, LocalDateTime toDateTime);

}
