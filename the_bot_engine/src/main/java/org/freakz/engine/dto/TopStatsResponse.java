package org.freakz.engine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.services.api.ServiceResponse;

import java.time.LocalDate;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class TopStatsResponse extends ServiceResponse {

    private LocalDate firstDay;
    private int totalDays;
    private int statDays;

}
