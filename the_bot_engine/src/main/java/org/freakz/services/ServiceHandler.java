package org.freakz.services;

public interface ServiceHandler {
    <T extends ServiceResponse> ServiceResponse handleServiceRequest(ServiceRequest request);
}
