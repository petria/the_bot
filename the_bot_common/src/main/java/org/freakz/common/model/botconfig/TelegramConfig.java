package org.freakz.common.model.botconfig;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class TelegramConfig {

  private String telegramName;

  private String token;

  private List<Channel> channelList;

  private boolean connectStartup;
}
