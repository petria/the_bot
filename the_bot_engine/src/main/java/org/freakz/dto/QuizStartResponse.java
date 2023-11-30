package org.freakz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.freakz.services.api.ServiceResponse;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class QuizStartResponse extends ServiceResponse {

    private String response;

}
