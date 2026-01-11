package org.freakz.engine.services.data;

import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.DataControllerService;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.data.repository.DataSavingService;
import org.freakz.engine.dto.DataSaverListResponse;
import org.freakz.engine.services.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.DataSaverList)
public class DataSaverListService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(DataSaverListService.class);

  @Override
  public void initializeService(ConfigService configService) throws Exception {
  }

  @Override
  public <T extends ServiceResponse> DataSaverListResponse handleServiceRequest(
      ServiceRequest request) {

    ApplicationContext applicationContext = request.getApplicationContext();
    DataControllerService service = applicationContext.getBean(DataControllerService.class);

    DataSaverListResponse response = DataSaverListResponse.builder().build();
    List<DataSaverInfo> list = new ArrayList<>();
    response.setDataSaverInfoList(list);

    Map<String, DataSavingService> dataSavingServiceMap = service.getDataSavingServiceMap();
    dataSavingServiceMap
        .values()
        .forEach(
            s -> {
              list.add(s.getDataSaverInfo());
            });

    return response;
  }
}
