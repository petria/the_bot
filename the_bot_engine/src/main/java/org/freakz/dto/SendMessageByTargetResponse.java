package org.freakz.dto;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.freakz.services.api.ServiceResponse;

@Builder
@Data
@ToString
public class SendMessageByTargetResponse extends ServiceResponse {

    private String sendTo;

}
