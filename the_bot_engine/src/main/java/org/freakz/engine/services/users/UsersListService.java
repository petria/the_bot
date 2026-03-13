package org.freakz.engine.services.users;

import org.freakz.common.model.users.User;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.service.UsersService;
import org.freakz.engine.dto.UsersResponse;
import org.freakz.engine.services.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.UsersListService)
@SuppressWarnings("unchecked")
public class UsersListService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(UsersListService.class);

  @Override
  public void initializeService(ConfigService configService) throws Exception {
  }

  @Override
  public <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request) {
    ApplicationContext applicationContext = request.getApplicationContext();
    UsersService bean = applicationContext.getBean(UsersService.class);

    UsersResponse response = UsersResponse.builder().userList((List<User>) bean.findAll()).build();

    return response;
  }
}
