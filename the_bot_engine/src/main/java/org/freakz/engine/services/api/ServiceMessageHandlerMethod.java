package org.freakz.engine.services.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Created by Petri Airio on 17.11.2015. - */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ServiceMessageHandlerMethod {

  ServiceRequestType ServiceRequestType();
}
