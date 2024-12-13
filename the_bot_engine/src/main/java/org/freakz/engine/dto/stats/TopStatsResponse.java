package org.freakz.engine.dto.stats;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.freakz.engine.services.api.ServiceResponse;

import java.util.Map;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class TopStatsResponse extends ServiceResponse {

  private Map<String, StatsNode> nodeMap;
}
