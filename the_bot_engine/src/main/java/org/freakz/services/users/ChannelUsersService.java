package org.freakz.services.users;


import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.dto.ChannelUsersResponse;
import org.freakz.services.AbstractService;
import org.freakz.services.ServiceMessageHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceRequestType;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_TARGET_ALIAS;

@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.GetChannelUsers)
@SuppressWarnings("unchecked")
public class ChannelUsersService extends AbstractService {
    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @Override
    public ChannelUsersResponse handleServiceRequest(ServiceRequest request) {
        ChannelUsersResponse response
                = ChannelUsersResponse.builder()
                .response("Channel users: " + request.getResults().getString(ARG_TARGET_ALIAS))
                .build();
        return response;
    }
}
