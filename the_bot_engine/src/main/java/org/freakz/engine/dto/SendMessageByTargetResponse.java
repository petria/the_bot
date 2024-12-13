package org.freakz.engine.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.freakz.engine.services.api.ServiceResponse;

@Builder
@Data
@ToString
@EqualsAndHashCode(callSuper = false)
public class SendMessageByTargetResponse extends ServiceResponse {

  private String sendTo;
}
