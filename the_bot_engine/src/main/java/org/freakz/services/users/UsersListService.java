package org.freakz.services.users;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.users.User;
import org.freakz.config.ConfigService;
import org.freakz.data.service.UsersService;
import org.freakz.dto.UsersResponse;
import org.freakz.services.api.*;
import org.springframework.context.ApplicationContext;

import java.util.List;

@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.UsersListService)
@SuppressWarnings("unchecked")
public class UsersListService extends AbstractService {

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @Override
    public <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request) {
        ApplicationContext applicationContext = request.getApplicationContext();
        UsersService bean = applicationContext.getBean(UsersService.class);

        UsersResponse response
                = UsersResponse.builder()
                .userList((List<User>) bean.findAll())
                .build();


        return response;
    }
}
