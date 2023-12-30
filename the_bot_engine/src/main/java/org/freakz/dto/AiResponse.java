package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.services.api.ServiceResponse;


@Builder
@EqualsAndHashCode(callSuper = false)
@Data
public class AiResponse extends ServiceResponse {
    private String result;
}
