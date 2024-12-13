package org.freakz.engine.services.users;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.dto.UsersResponse;
import org.freakz.engine.services.api.*;
import org.springframework.context.ApplicationContext;

@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.UsersListService)
@SuppressWarnings("unchecked")
public class UsersListService extends AbstractService {

  @Override
  public void initializeService(ConfigService configService) throws Exception {}

  @Override
  public <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request) {
    ApplicationContext applicationContext = request.getApplicationContext();
    UsersService bean = applicationContext.getBean(UsersService.class);

    UsersResponse response = UsersResponse.builder().userList((List<User>) bean.findAll()).build();

    return response;
  }
}
