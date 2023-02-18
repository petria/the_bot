package org.freakz.services;

import lombok.Builder;
import lombok.Data;
import org.freakz.common.model.json.engine.EngineRequest;

@Builder
@Data
public class ServiceRequest {

    private EngineRequest engineRequest;

}
