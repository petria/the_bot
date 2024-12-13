package org.freakz.engine.data.service;

import java.util.List;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.users.User;

public interface UsersService {

  List<? extends DataNodeBase> findAll();

  User getNotKnownUser();
}
