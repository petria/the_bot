package org.freakz.engine.services.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.data.DataControllerService;
import org.freakz.engine.data.repository.DataSaverInfo;
import org.freakz.engine.data.repository.DataSavingService;
import org.freakz.engine.dto.DataSaverListResponse;
import org.freakz.engine.services.api.*;
import org.springframework.context.ApplicationContext;

@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.DataSaverList)
public class DataSaverListService extends AbstractService {

  @Override
  public void initializeService(ConfigService configService) throws Exception {}

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
