package org.freakz.engine.services;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.freakz.engine.config.ConfigService;
import org.freakz.engine.services.api.*;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executor;

import static org.reflections.scanners.Scanners.SubTypes;
import static org.reflections.scanners.Scanners.TypesAnnotated;

@Service
@Slf4j
@SuppressWarnings("unchecked")
public class HokanServices {

    private final Executor executor;

    private final ConfigService configService;

    public HokanServices(@Qualifier("Services") Executor executor, ConfigService configService, ApplicationContext applicationContext) {
        this.executor = executor;
        this.configService = configService;
        this.applicationContext = applicationContext;

    }

    private final ApplicationContext applicationContext;


    @PostConstruct
    public void runInitializeService() throws Exception {
        log.debug("Finding ServiceMessageHandlers...");
        Set<Class<?>> serviceMessageHandlers = reflections.getTypesAnnotatedWith(ServiceMessageHandler.class);
        log.debug("... found ServiceMessageHandler: {}", serviceMessageHandlers.size());

        log.debug("Finding ServiceMethodHandlers...");
        Set<Class<?>> serviceMethodHandlers = reflections.getTypesAnnotatedWith(ServiceMethodHandler.class);
        log.debug("... found ServiceMethodHandler: {}", serviceMessageHandlers.size());

        List<Class<?>> allHandlers = new ArrayList<>(serviceMessageHandlers);
        allHandlers.addAll(serviceMethodHandlers);

        log.debug("Starting initialize all Services, count: {}", allHandlers.size());

        for (Class<?> aClass : allHandlers) {
            AbstractService service = (AbstractService) aClass.getConstructor().newInstance();
            this.executor.execute(() -> {
                try {
                    log.debug("Init services: " + service.getClass().getSimpleName());

                    Thread.currentThread().setName(Thread.currentThread().getName() + "-" + service.getClass().getSimpleName());
                    service.initializeService(configService);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

    public <T extends ServiceResponse> T doServiceRequestMethods(ServiceRequest request, ServiceRequestType serviceRequestType) {
        try {
            Map<Class<?>, List<MethodAndType>> methodsMap = findServiceMessageHandlerMethods(serviceRequestType);
            request.setApplicationContext(applicationContext);
            for (Class<?> aClass : methodsMap.keySet()) {
                AbstractService service = null; //(AbstractService) aClass.getConstructor().newInstance();
                List<MethodAndType> methods = methodsMap.get(aClass);
                for (MethodAndType method : methods) {
                    if (method.isSpring) {
                        Object bean = applicationContext.getBean(aClass);
                        Object obj = method.method.invoke(bean, request);
                        return (T) obj;
                    } else {
                        service = (AbstractService) aClass.getConstructor().newInstance();
                        Object obj = method.method.invoke(service, request);
                        return (T) obj;
                    }
                }

            }
        } catch (Exception e) {
            log.error("Service handler failure", e);
        }
        return (T) new ServiceResponse();
    }

    public <T extends ServiceResponse> T doServiceRequest(ServiceRequest request, ServiceRequestType serviceRequestType) {
        try {

            ServiceHandler serviceHandler = findServiceMessageHandlers(serviceRequestType);


            if (serviceHandler != null) {
                request.setApplicationContext(applicationContext);
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


    private final Reflections reflections = new Reflections(ClasspathHelper.forPackage("org.freakz"));

    class MethodAndType {
        Method method;
        boolean isSpring;
    }

    private Map<Class<?>, List<MethodAndType>> findServiceMessageHandlerMethods(ServiceRequestType serviceRequestType) {


        Map<Class<?>, List<MethodAndType>> methodsMap2 = new HashMap<>();

        Set<Class<?>> annotated =
                reflections.get(SubTypes.of(TypesAnnotated.with(ServiceMethodHandler.class)).asClass());


        for (Class<?> aClass : annotated) {
            Method[] methods = aClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(ServiceMessageHandlerMethod.class)) {
                    ServiceMessageHandlerMethod annotation = method.getAnnotation(ServiceMessageHandlerMethod.class);
                    ServiceRequestType annotatedType = annotation.ServiceRequestType();
                    if (serviceRequestType.equals(annotatedType)) {
                        List<MethodAndType> list2 = methodsMap2.computeIfAbsent(aClass, k -> new ArrayList<>());
                        MethodAndType methodAndType = new MethodAndType();
                        methodAndType.method = method;
                        methodAndType.isSpring = false;
                        list2.add(methodAndType);
                    }
                }
            }
        }

        Set<Class<?>> annotatedSpring =
                reflections.get(SubTypes.of(TypesAnnotated.with(SpringServiceMethodHandler.class)).asClass());
        for (Class<?> aClass : annotatedSpring) {
            Method[] methods = aClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(ServiceMessageHandlerMethod.class)) {
                    ServiceMessageHandlerMethod annotation = method.getAnnotation(ServiceMessageHandlerMethod.class);
                    ServiceRequestType annotatedType = annotation.ServiceRequestType();
                    if (serviceRequestType.equals(annotatedType)) {
                        List<MethodAndType> list2 = methodsMap2.computeIfAbsent(aClass, k -> new ArrayList<>());
                        MethodAndType methodAndType = new MethodAndType();
                        methodAndType.method = method;
                        methodAndType.isSpring = true;
                        list2.add(methodAndType);
                    }
                }
            }
        }

        return methodsMap2;
    }

    private ServiceHandler findServiceMessageHandlers(ServiceRequestType serviceRequestType) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(ServiceMessageHandler.class);
        for (Class<?> aClass : typesAnnotatedWith) {
            ServiceMessageHandler annotation = aClass.getAnnotation(ServiceMessageHandler.class);
            ServiceRequestType annotatedType = annotation.ServiceRequestType();
            if (serviceRequestType.equals(annotatedType)) {
                return (AbstractService) aClass.getConstructor().newInstance();
            }
        }
        return null;
    }

}
