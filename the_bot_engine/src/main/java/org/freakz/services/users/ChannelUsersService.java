package org.freakz.services.users;


import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.connectionmanager.ChannelUsersByTargetAliasResponse;
import org.freakz.config.ConfigService;
import org.freakz.dto.ChannelUsersResponse;
import org.freakz.services.AbstractService;
import org.freakz.services.ServiceMessageHandler;
import org.freakz.services.ServiceRequest;
import org.freakz.services.ServiceRequestType;
import org.freakz.services.connections.ConnectionManagerService;
import org.springframework.context.ApplicationContext;

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

        ApplicationContext applicationContext = request.getApplicationContext();
        ConnectionManagerService cms = applicationContext.getBean(ConnectionManagerService.class);

        String targetAlias = request.getResults().getString(ARG_TARGET_ALIAS);

        ChannelUsersByTargetAliasResponse ioResponse = cms.getChannelUsersByTargetAlias(targetAlias);

        final StringBuilder users = new StringBuilder(String.format("%s users: ", targetAlias));
        ioResponse.getChannelUsers().forEach(user -> {
            users.append(user.getNick());
            users.append(" ");
        });


        ChannelUsersResponse response
                = ChannelUsersResponse.builder()
                .response(users.toString())
                .build();
        return response;
    }
}
