package org.freakz.data.service;

import org.freakz.common.model.users.User;

import java.util.List;

public interface UsersService {

    List<User> findAll();

    User getNotKnownUser();
}
