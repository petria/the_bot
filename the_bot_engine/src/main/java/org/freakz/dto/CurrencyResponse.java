package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.services.api.ServiceResponse;


@Builder
@Data
public class CurrencyResponse extends ServiceResponse {
    private double amount;
    private String from;
    private String to;
}
