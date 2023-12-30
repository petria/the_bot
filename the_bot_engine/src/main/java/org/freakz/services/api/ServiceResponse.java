package org.freakz.services.api;


import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = false)
public class ServiceResponse {

    private String status = "NOK: not implemented";


}
