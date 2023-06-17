package org.freakz.services.users;

import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.data.service.UsersService;
import org.freakz.dto.UsersResponse;
import org.freakz.services.*;
import org.springframework.context.ApplicationContext;

@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.UsersListService)
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
                .userList(bean.findAll())
                .build();


        return response;
    }
}
