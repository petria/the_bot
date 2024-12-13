package org.freakz.engine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.services.api.ServiceResponse;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class CurrencyResponse extends ServiceResponse {
    private double amount;
    private String from;
    private String to;
}
