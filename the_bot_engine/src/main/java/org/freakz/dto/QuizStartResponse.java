package org.freakz.dto;

import lombok.*;
import org.freakz.services.api.ServiceResponse;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class QuizStartResponse extends ServiceResponse {

    private String response;

}
