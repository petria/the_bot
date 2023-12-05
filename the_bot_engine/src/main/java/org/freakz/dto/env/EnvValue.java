package org.freakz.dto.env;

import lombok.*;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EnvValue {

    private String key;
    private String value;

}
