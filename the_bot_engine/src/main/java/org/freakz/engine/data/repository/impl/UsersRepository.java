package org.freakz.engine.data.repository.impl;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.users.User;
import org.freakz.engine.data.repository.DataBaseRepository;

import java.util.List;

public interface UsersRepository extends DataBaseRepository<User> {

  List<? extends DataNodeBase> findAll();
}
