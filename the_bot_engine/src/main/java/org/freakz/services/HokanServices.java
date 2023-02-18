package org.freakz.services;

import org.freakz.services.kelikamerat.KeliKameratService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HokanServices {
    enum RequestHandler {
        KeliKameratService
    }

    //    class ServiceRequest<R extends ServiceResponse> {
//    }

    @Autowired
    KeliKameratService keliKameratService;


    public ServiceResponse<?> handleServiceRequest(ServiceRequest request) {
        ServiceResponse<?> serviceResponse = keliKameratService.handleServiceRequest(request);

        return serviceResponse;
    }


}
