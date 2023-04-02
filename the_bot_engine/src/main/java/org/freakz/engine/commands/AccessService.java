package org.freakz.engine.commands;

import org.freakz.common.model.json.engine.EngineRequest;
import org.springframework.stereotype.Service;

@Service
public class AccessService {


    public User getUser(EngineRequest request) {
        User user
                = User.builder()
                .isAdmin(checkIsFromAdmin(request))
                .build();

        return user;
    }

    private boolean checkIsFromAdmin(EngineRequest request) {
        if (request.getNetwork().equals("IRCNet")) {
            return request.getFromSender().equals("_Pete_");
        }
        return false;
    }


}
