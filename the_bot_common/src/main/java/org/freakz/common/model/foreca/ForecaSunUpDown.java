package org.freakz.common.model.foreca;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ForecaSunUpDown {

    private int dayLengthTotalMinutes;
    private int dayLengthHours;
    private int dayLengthMinutes;

    private String sunUpTime;

    private String sunDownTime;
}
