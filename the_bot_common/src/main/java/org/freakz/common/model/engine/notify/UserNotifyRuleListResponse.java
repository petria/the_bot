package org.freakz.common.model.engine.notify;

import java.util.ArrayList;
import java.util.List;

public class UserNotifyRuleListResponse {

  private List<UserNotifyRule> rules = new ArrayList<>();

  public UserNotifyRuleListResponse() {
  }

  public UserNotifyRuleListResponse(List<UserNotifyRule> rules) {
    this.rules = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
  }

  public List<UserNotifyRule> getRules() {
    return rules;
  }

  public void setRules(List<UserNotifyRule> rules) {
    this.rules = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
  }
}
