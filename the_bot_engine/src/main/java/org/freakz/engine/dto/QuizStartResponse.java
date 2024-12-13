package org.freakz.engine.dto;

import lombok.*;
import org.freakz.engine.services.api.ServiceResponse;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class QuizStartResponse extends ServiceResponse {

  private String response;
}
