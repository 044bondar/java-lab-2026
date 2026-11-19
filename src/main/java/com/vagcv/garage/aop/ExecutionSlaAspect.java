package com.vagcv.garage.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ExecutionSlaAspect {

    private final DealAuditRegistry auditRegistry;

    @Around("@annotation(trackExecutionSla)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, TrackExecutionSla trackExecutionSla) throws Throwable {
        String operation = trackExecutionSla.operationName().isBlank()
                ? joinPoint.getSignature().toShortString()
                : trackExecutionSla.operationName();

        long startNs = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            boolean violated = elapsedMs > trackExecutionSla.maxMillis();

            auditRegistry.recordSla(new DealAuditRegistry.SlaMetric(operation, elapsedMs, violated));

            if (violated) {
                log.warn("[AOP SLA VIOLATION] Операція '{}' виконувалася {} мс (ліміт SLA: {} мс)",
                        operation, elapsedMs, trackExecutionSla.maxMillis());
            } else {
                log.info("[AOP SLA OK] Операція '{}' виконана за {} мс", operation, elapsedMs);
            }
        }
    }
}