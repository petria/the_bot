package org.freakz.engine.data.service;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;

import java.util.List;
import java.util.function.UnaryOperator;

public interface UsersService {

  List<? extends DataNodeBase> findAll();

  User getNotKnownUser();

  User addChatIdentity(long userId, UserChatIdentity identity, boolean moveIfOwned);

  default User updateByUsername(String username, UnaryOperator<User> updater) {
    throw new UnsupportedOperationException("User updates are not supported by this UsersService");
  }

  void reloadUsers();
}
