package org.freakz.services;


public class ServiceResponse<T> {

    private T response;

    public ServiceResponse(T response) {
        this.response = response;
    }

    public T getResponse() {
        return response;
    }
}
