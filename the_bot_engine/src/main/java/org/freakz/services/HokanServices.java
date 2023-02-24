package org.freakz.services;

import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class HokanServices {

    public HokanServices() {
        scanServiceMessageHandlers();
    }

//    @Autowired
//    KeliKameratService keliKameratService;


    public <T extends ServiceResponse> T doServiceRequest(ServiceRequest request, RequestHandler requestHandler) {

        return null; //keliKameratService.doService(request);
    }


    private void scanServiceMessageHandlers() {
        Reflections reflections = new Reflections("org.freakz.services");
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(ServiceMessageHandler.class);
        typesAnnotatedWith.forEach(
                aClass -> {
                    ServiceMessageHandler annotation = aClass.getAnnotation(ServiceMessageHandler.class);
                    ServiceRequestType serviceRequestType = annotation.ServiceRequestType();
                    int foo = 0;
                }
        );

    }

}
