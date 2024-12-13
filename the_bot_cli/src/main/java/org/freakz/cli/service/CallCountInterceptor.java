package org.freakz.cli.service;

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

  @Getter private final ConcurrentMap<String, Integer> callCounts = new ConcurrentHashMap<>();

  @Around("execution(* org.freakz.cli.clients.*.*(..))")
  public Object captureMessageSendingMethod(ProceedingJoinPoint jp) throws Throwable {
    String methodName = "OUT: " + jp.getSignature().getName();
    callCounts.compute(methodName, (key, val) -> val == null ? 1 : val + 1);
    methodName = "IN: " + jp.getSignature().getName();

    callCounts.compute(methodName, (key, val) -> val == null ? 1 : val + 1);

    return jp.proceed();
  }
}
