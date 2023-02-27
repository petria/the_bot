package org.freakz.services;

import com.martiansoftware.jsap.JSAPResult;
import lombok.Builder;
import lombok.Data;
import org.freakz.common.model.json.engine.EngineRequest;

@Builder
@Data
public class ServiceRequest {

    private EngineRequest engineRequest;

    private JSAPResult results;

}
