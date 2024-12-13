package org.freakz.engine.services.topcounter;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataValuesModel;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.dto.stats.TopCountsResponse;
import org.freakz.engine.services.api.*;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_CHANNEL;
import static org.freakz.engine.commands.util.StaticArgumentStrings.ARG_TOP_KEY;

@Slf4j
@ServiceMessageHandler(ServiceRequestType = ServiceRequestType.GetTopCountsService)
public class GetTopCountsService extends AbstractService {
  @Override
  public void initializeService(ConfigService configService) throws Exception {}

  @Override
  public <T extends ServiceResponse> TopCountsResponse handleServiceRequest(
      ServiceRequest request) {

    String key = request.getResults().getString(ARG_TOP_KEY);
    String channel =
        request
            .getResults()
            .getString(ARG_CHANNEL, request.getEngineRequest().getReplyTo())
            .toLowerCase();
    String network = request.getEngineRequest().getNetwork().toLowerCase();

    ApplicationContext applicationContext = request.getApplicationContext();
    TopCountService service = applicationContext.getBean(TopCountService.class);
    List<DataValuesModel> dataValues = service.getDataValuesAsc(channel, network, key);

    TopCountsResponse response = TopCountsResponse.builder().build();
    response.setDataValues(dataValues);

    return response;
  }
}
