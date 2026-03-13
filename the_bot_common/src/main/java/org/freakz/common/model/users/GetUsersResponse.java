package org.freakz.common.model.users;

import java.util.List;
import java.util.Objects;

public class GetUsersResponse {

  private List<User> users;

  public GetUsersResponse() {
  }

  public GetUsersResponse(List<User> users) {
    this.users = users;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<User> getUsers() {
    return users;
  }

  public void setUsers(List<User> users) {
    this.users = users;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GetUsersResponse that = (GetUsersResponse) o;
    return Objects.equals(users, that.users);
  }

  @Override
  public int hashCode() {
    return Objects.hash(users);
  }

  @Override
  public String toString() {
    return "GetUsersResponse{" +
        "users=" + users +
        '}';
  }

  public static class Builder {
    private List<User> users;

    public Builder users(List<User> users) {
      this.users = users;
      return this;
    }

    public GetUsersResponse build() {
      return new GetUsersResponse(users);
    }
  }
}
