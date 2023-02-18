package org.freakz.services;

public interface ServiceRequestHandler {

    <T> ServiceResponse doService(ServiceRequest request, Class<T> clazz, RequestHandler requestHandler);

}
