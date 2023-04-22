package org.freakz.data;

import lombok.extern.slf4j.Slf4j;
import org.freakz.data.repository.DataSavingService;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@Slf4j
public class DataControllerService {

    private final ApplicationContext context;

    public DataControllerService(ApplicationContext context) {
        this.context = context;
    }

    private Collection<DataSavingService> getDataSavingServices() {
        Map<String, DataSavingService> beansOfType = context.getBeansOfType(DataSavingService.class);
        return beansOfType.values();
    }


    @Scheduled(fixedRate = 1000)
    public void repositorySaveTimer() {
        for (DataSavingService service : getDataSavingServices()) {
            service.checkIsSavingNeeded();
        }
    }

}
