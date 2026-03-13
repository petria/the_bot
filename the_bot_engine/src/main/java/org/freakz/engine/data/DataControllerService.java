package org.freakz.engine.data;

import org.freakz.engine.data.repository.DataSavingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class DataControllerService {

  private static final Logger log = LoggerFactory.getLogger(DataControllerService.class);

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

  public Map<String, DataSavingService> getDataSavingServiceMap() {
    return context.getBeansOfType(DataSavingService.class);
  }
}
