package com.vagcv.garage.aop;

import com.vagcv.garage.dto.VehicleResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DealAuditAspect {

    private final DealAuditRegistry auditRegistry;

    @AfterReturning(pointcut = "@annotation(auditDealOperation)", returning = "result")
    public void recordDealEvent(AuditDealOperation auditDealOperation, Object result) {
        if (result instanceof VehicleResponseDto dto) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String operator = (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";

            DealAuditRegistry.AuditEntry entry = new DealAuditRegistry.AuditEntry(
                    auditDealOperation.action(),
                    dto.getId(),
                    dto.getVin(),
                    operator,
                    LocalDateTime.now()
            );

            auditRegistry.recordAudit(entry);
            log.info("[AOP AUDIT] Дія='{}', авто ID={}, VIN={}, оператор='{}'",
                    entry.action(), entry.vehicleId(), entry.vin(), entry.operator());
        }
    }
}