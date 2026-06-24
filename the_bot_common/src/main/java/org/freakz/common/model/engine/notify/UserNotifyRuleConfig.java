package org.freakz.common.model.engine.notify;

import java.util.ArrayList;
import java.util.List;

public class UserNotifyRuleConfig {

  private List<UserNotifyRule> rules = new ArrayList<>();

  public UserNotifyRuleConfig() {
  }

  public UserNotifyRuleConfig(List<UserNotifyRule> rules) {
    this.rules = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
  }

  public List<UserNotifyRule> getRules() {
    return rules;
  }

  public void setRules(List<UserNotifyRule> rules) {
    this.rules = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
  }
}
