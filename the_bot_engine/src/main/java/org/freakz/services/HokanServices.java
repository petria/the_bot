package org.freakz.services;

import org.freakz.services.kelikamerat.KeliKameratService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HokanServices {

    //    class ServiceRequest<R extends ServiceResponse> {
//    }

    @Autowired
    KeliKameratService keliKameratService;


    public <T extends ServiceResponse> T doServiceRequest(ServiceRequest request, RequestHandler requestHandler) {

        return keliKameratService.doService(request);
    }

}
