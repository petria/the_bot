package org.freakz.services;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

@Service
@Slf4j
public class HokanServices {


    public <T extends ServiceResponse> T doServiceRequest(ServiceRequest request, ServiceRequestType serviceRequestType) {
        try {
            ServiceHandler serviceHandler = findServiceMessageHandlers(serviceRequestType);
            if (serviceHandler != null) {
                return (T) serviceHandler.handleServiceRequest(request);
            } else {
                log.error("Service handler not found for: {}", serviceRequestType);
                return null;
            }
        } catch (Exception e) {
            log.error("Service handler failure", e);
        }
        return null;
    }


    private ServiceHandler findServiceMessageHandlers(ServiceRequestType serviceRequestType) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Reflections reflections = new Reflections("org.freakz.services");
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(ServiceMessageHandler.class);
        for (Class<?> aClass : typesAnnotatedWith) {
            ServiceMessageHandler annotation = aClass.getAnnotation(ServiceMessageHandler.class);
            ServiceRequestType annotatedType = annotation.ServiceRequestType();
            if (serviceRequestType.equals(annotatedType)) {
                return (ServiceHandler) aClass.getConstructor().newInstance();
            }
        }
        return null;
    }

}
