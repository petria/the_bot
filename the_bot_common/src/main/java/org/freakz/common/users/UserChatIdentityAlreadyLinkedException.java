package org.freakz.common.users;

import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;

public class UserChatIdentityAlreadyLinkedException extends IllegalArgumentException {

  private final Long ownerUserId;
  private final String ownerUsername;
  private final String ownerName;
  private final String identityKey;

  public UserChatIdentityAlreadyLinkedException(User owner, UserChatIdentity identity) {
    super("Chat identity is already linked to user: " + (owner == null ? "unknown" : owner.getUsername()));
    this.ownerUserId = owner == null ? null : owner.getId();
    this.ownerUsername = owner == null ? null : owner.getUsername();
    this.ownerName = owner == null ? null : owner.getName();
    this.identityKey = UserChatIdentityUtil.identityKey(identity);
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public String getOwnerUsername() {
    return ownerUsername;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public String getIdentityKey() {
    return identityKey;
  }
}
