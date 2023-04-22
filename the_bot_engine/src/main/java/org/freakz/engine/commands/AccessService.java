package org.freakz.engine.commands;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.json.engine.EngineRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccessService {


    public User getUser(EngineRequest request) {
        User user
                = User.builder()
                .isAdmin(checkIsFromAdmin(request))
                .build();

        return user;
    }

    private boolean checkIsFromAdmin(EngineRequest request) {
        log.debug("Check admin access: {}", request);
        boolean isAdmin = false;
        if (request.getNetwork().equalsIgnoreCase("IRCNet")) {
            isAdmin = request.getFromSender().equals("_Pete_");
        }
        if (request.getNetwork().equalsIgnoreCase("TelegramNetwork")) {
            if (request.getFromSenderId().equals("138695441")) {
                isAdmin = true;
            }
        }
        if (request.getNetwork().equalsIgnoreCase("Discord")) {
            if (request.getFromSenderId().equals("265828694445129728")) {
                isAdmin = true;
            }
        }
        log.debug("Check admin result: {}", isAdmin);
        return isAdmin;
    }


}
