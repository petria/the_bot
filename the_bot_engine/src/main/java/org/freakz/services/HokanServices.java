package org.freakz.services;

import lombok.extern.slf4j.Slf4j;
import org.freakz.config.ConfigService;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class HokanServices {

    private final Executor executor;

    private final ConfigService configService;

    public HokanServices(@Qualifier("Services") Executor executor, ConfigService configService) {
        this.executor = executor;
        this.configService = configService;
    }

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

    @PostConstruct
    public void runInitializeService() throws Exception {
        log.debug("Initializing all services...");
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(ServiceMessageHandler.class);
        for (Class<?> aClass : typesAnnotatedWith) {
            AbstractService service = (AbstractService) aClass.getConstructor().newInstance();
            this.executor.execute(() -> {
                try {
                    log.debug("Init services: " + service.getClass().getSimpleName());
                    service.initializeService(configService);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

    private Reflections reflections = new Reflections("org.freakz.services");

    private ServiceHandler findServiceMessageHandlers(ServiceRequestType serviceRequestType) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(ServiceMessageHandler.class);
        for (Class<?> aClass : typesAnnotatedWith) {
            ServiceMessageHandler annotation = aClass.getAnnotation(ServiceMessageHandler.class);
            ServiceRequestType annotatedType = annotation.ServiceRequestType();
            if (serviceRequestType.equals(annotatedType)) {
                AbstractService service = (AbstractService) aClass.getConstructor().newInstance();
                return service;
            }
        }
        return null;
    }

}
