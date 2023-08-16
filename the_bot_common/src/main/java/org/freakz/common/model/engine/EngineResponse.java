package org.freakz.common.model.engine;

import lombok.*;
import org.freakz.common.model.users.User;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
@ToString
public class EngineResponse {

    private String message;

    private User user;

}
