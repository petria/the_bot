package org.freakz.common.model.slack;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class Authorization {
  private String enterpriseId;
  private String teamId;
  private String userId;
  private boolean isBot;
  private boolean isEnterpriseInstall;
}
