package org.freakz.common.model.connectionmanager;

import lombok.Data;

@Data
public class SendIrcRawMessageByTargetAliasResponse {

  private String sentTo;

  private String serverResponse;
}
