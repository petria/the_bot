package org.freakz.common.model.connectionmanager;

import java.util.List;

public class GetKnownChatUsersResponse {

  private List<KnownChatUserResponse> users;

  public GetKnownChatUsersResponse() {
  }

  public GetKnownChatUsersResponse(List<KnownChatUserResponse> users) {
    this.users = users;
  }

  public List<KnownChatUserResponse> getUsers() {
    return users;
  }

  public void setUsers(List<KnownChatUserResponse> users) {
    this.users = users;
  }
}
