package org.freakz.engine.services.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Created by Petri Airio on 17.11.2015. - */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceMessageHandlerMethods {

  ServiceMessageHandlerMethod[] value();
}
