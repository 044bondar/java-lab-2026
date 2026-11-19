package com.vagcv.garage.aop;

import com.vagcv.garage.dto.VehicleRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Aspect
@Component
public class HighValueResaleGuardAspect {

    @Before("@annotation(guardHighValueResale)")
    public void checkHighValueResalePermission(JoinPoint joinPoint, GuardHighValueResale guardHighValueResale) {
        BigDecimal threshold = BigDecimal.valueOf(guardHighValueResale.threshold());

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof VehicleRequestDto dto && dto.getTargetResalePrice() != null) {
                if (dto.getTargetResalePrice().compareTo(threshold) > 0) {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    boolean isAdmin = auth != null && auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                    if (!isAdmin) {
                        log.warn("[AOP SECURITY GUARD] Блокування створення преміум-лоту ({} > {}): відсутня роль ROLE_ADMIN",
                                dto.getTargetResalePrice(), threshold);
                        throw new AccessDeniedException(
                                String.format("Для виставлення авто дорожче %s потрібна роль ROLE_ADMIN", threshold)
                        );
                    }
                }
            }
        }
    }
}