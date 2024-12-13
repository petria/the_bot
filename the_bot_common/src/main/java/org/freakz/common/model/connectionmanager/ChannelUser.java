package org.freakz.common.model.connectionmanager;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ChannelUser {

  private String account;

  private String awayMessage;

  private String host;

  private String nick;

  private String operatorInformation;

  private String realName;

  private String server;

  private String userString;

  private boolean isAway;
}
