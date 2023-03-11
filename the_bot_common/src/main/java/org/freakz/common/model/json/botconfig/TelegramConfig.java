package org.freakz.common.model.json.botconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class TelegramConfig {

    private String token;

    private boolean connectStartup;

}
