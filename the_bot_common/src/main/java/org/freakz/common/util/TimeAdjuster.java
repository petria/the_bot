package org.freakz.common.util;

import java.time.LocalDateTime;

public class TimeAdjuster {

    public static LocalDateTime getAdjustedLocalDateTime(int adjustMinutes) {
        LocalDateTime time = LocalDateTime.now().plusMinutes(adjustMinutes);
        return time;
    }
}
