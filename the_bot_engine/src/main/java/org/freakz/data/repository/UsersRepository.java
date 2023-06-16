package org.freakz.data.repository;

import org.freakz.common.model.users.User;

import java.util.List;

public interface UsersRepository extends DataBaseRepository<User> {

    List<User> findAll();

}
