package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import org.freakz.services.api.ServiceResponse;

@Builder
@Data
public class IrcOPResponse extends ServiceResponse {

    private String response;

}
