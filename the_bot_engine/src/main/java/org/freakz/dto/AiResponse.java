package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.services.ServiceResponse;

@Builder
@Data
public class AiResponse extends ServiceResponse {
    private String result;
}
