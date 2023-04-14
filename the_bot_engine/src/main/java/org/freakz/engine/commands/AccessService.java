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
        if (request.getNetwork().equalsIgnoreCase("IRCNet")) {
            return request.getFromSender().equals("_Pete_");
        }
        if (request.getNetwork().equalsIgnoreCase("TelegramNetwork")) {
            if (request.getFromSenderId().equals("138695441")) {
                return true;
            }
        }
        if (request.getNetwork().equalsIgnoreCase("Discord")) {
            if (request.getFromSenderId().equals("265828694445129728")) {
                return true;
            }
        }
        return false;
    }


}
