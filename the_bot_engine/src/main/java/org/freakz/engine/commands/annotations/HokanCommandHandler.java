package org.freakz.engine.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Petri Airio on 17.11.2015.
 * -
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HokanCommandHandler {

//    ServiceRequestType ServiceRequestType();

}
