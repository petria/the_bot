package org.freakz.services;

import com.martiansoftware.jsap.JSAPResult;
import lombok.Builder;
import lombok.Data;
import org.freakz.common.model.json.engine.EngineRequest;
import org.springframework.context.ApplicationContext;

@Builder
@Data
public class ServiceRequest {

    private ApplicationContext applicationContext;
    private EngineRequest engineRequest;

    private JSAPResult results;

}
