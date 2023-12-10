package org.freakz.services.status;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Aspect
@Slf4j
public class CallCountInterceptor {


    @Getter
    private final ConcurrentMap<String, Integer> callCounts = new ConcurrentHashMap<>();

    @Around("execution(* org.freakz.clients.*.*(..))")
    public Object captureMessageSendingMethod(ProceedingJoinPoint jp) throws Throwable {
        String methodName = jp.getSignature().getName();
//        log.debug("sending #Captured methodName: {}", methodName);

        callCounts.compute(methodName, (key, val) -> val == null ? 1 : val + 1);

        return jp.proceed();
    }

    @Around("execution(* org.freakz.controller.*.*(..))")
    public Object captureMessageReceivingMethod(ProceedingJoinPoint jp) throws Throwable {
        String methodName = jp.getSignature().getName();
//        log.debug("receiving #Captured methodName: {}", methodName);

        callCounts.compute(methodName, (key, val) -> val == null ? 1 : val + 1);

        return jp.proceed();
    }

}
