package org.freakz.engine.services.api;

public abstract class AbstractService implements ServiceHandler {

    @Override
    public <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request) {
        throw new RuntimeException("AbstractService :: handleServiceRequest(ServiceRequest request) not correctly implemented!!");
    }

}
