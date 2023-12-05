package org.freakz.data.repository.impl;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.users.User;
import org.freakz.data.repository.DataBaseRepository;

import java.util.List;

public interface UsersRepository extends DataBaseRepository<User> {

    List<? extends DataNodeBase> findAll();

}
