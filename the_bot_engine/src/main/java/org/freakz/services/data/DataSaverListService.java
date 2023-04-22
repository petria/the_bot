package org.freakz.services.data;


import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.freakz.data.DataControllerService;
import org.freakz.data.repository.DataSaverInfo;
import org.freakz.dto.DataSaverListResponse;
import org.freakz.services.*;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;


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
        DataSaverInfo info = DataSaverInfo.builder().name("Testinfo").build();
        List<DataSaverInfo> list = new ArrayList<>();
        list.add(info);
        response.setDataSaverInfoList(list);
        return response;
    }
}
