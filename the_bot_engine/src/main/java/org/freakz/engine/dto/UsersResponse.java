package org.freakz.engine.dto;

import org.freakz.common.model.users.User;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.List;

public class UsersResponse extends ServiceResponse {

  private List<User> userList;

  public UsersResponse() {
  }

  public UsersResponse(List<User> userList) {
    this.userList = userList;
  }

  public List<User> getUserList() {
    return userList;
  }

  public void setUserList(List<User> userList) {
    this.userList = userList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UsersResponse that = (UsersResponse) o;

    return userList != null ? userList.equals(that.userList) : that.userList == null;
  }

  @Override
  public int hashCode() {
    return userList != null ? userList.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "UsersResponse{" +
        "userList=" + userList +
        '}';
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<User> userList;

    Builder() {
    }

    public Builder userList(List<User> userList) {
      this.userList = userList;
      return this;
    }

    public UsersResponse build() {
      return new UsersResponse(userList);
    }
  }
}
