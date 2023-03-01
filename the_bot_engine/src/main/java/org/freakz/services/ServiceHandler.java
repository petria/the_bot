package org.freakz.services;

public interface ServiceHandler {

    void initializeService() throws Exception;

    <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request);
}
