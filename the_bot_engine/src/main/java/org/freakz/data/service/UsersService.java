package org.freakz.data.service;

import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.users.User;

import java.util.List;

public interface UsersService {

    List<? extends DataNodeBase> findAll();

    User getNotKnownUser();
}
