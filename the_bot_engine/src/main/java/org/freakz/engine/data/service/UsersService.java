package org.freakz.engine.data.service;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.users.User;
import org.freakz.common.model.users.UserChatIdentity;

import java.util.List;

public interface UsersService {

  List<? extends DataNodeBase> findAll();

  User getNotKnownUser();

  User addChatIdentity(long userId, UserChatIdentity identity, boolean moveIfOwned);

  void reloadUsers();
}
