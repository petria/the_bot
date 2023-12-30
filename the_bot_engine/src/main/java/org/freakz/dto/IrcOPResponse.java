package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.services.api.ServiceResponse;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class IrcOPResponse extends ServiceResponse {

    private String response;

}
