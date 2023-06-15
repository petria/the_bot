package org.freakz.services.data;


import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.data.DataControllerService;
import org.freakz.data.repository.DataSaverInfo;
import org.freakz.data.repository.DataSavingService;
import org.freakz.dto.DataSaverListResponse;
import org.freakz.services.*;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.DataSaverList)
public class DataSaverListService extends AbstractService {

    @Override
    public void initializeService(ConfigService configService) throws Exception {

    }

    @Override
    public <T extends ServiceResponse> DataSaverListResponse handleServiceRequest(ServiceRequest request) {

        ApplicationContext applicationContext = request.getApplicationContext();
        DataControllerService service = applicationContext.getBean(DataControllerService.class);

        DataSaverListResponse response = DataSaverListResponse.builder().build();
        List<DataSaverInfo> list = new ArrayList<>();
        response.setDataSaverInfoList(list);

        Map<String, DataSavingService> dataSavingServiceMap = service.getDataSavingServiceMap();
        dataSavingServiceMap.values().forEach(s -> {
            list.add(s.getDataSaverInfo());
        });

        return response;
    }
}
