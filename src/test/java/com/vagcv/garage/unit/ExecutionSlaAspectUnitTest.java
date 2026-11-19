package com.vagcv.garage.unit;

import com.vagcv.garage.aop.DealAuditRegistry;
import com.vagcv.garage.aop.ExecutionSlaAspect;
import com.vagcv.garage.aop.TrackExecutionSla;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Lab 4 (Unit): Пряме тестування ExecutionSlaAspect без контексту Spring")
class ExecutionSlaAspectUnitTest {

    private DealAuditRegistry registry;
    private ExecutionSlaAspect aspect;
    private ProceedingJoinPoint joinPoint;
    private TrackExecutionSla annotation;

    @BeforeEach
    void setUp() {
        registry = new DealAuditRegistry();
        aspect = new ExecutionSlaAspect(registry);
        joinPoint = mock(ProceedingJoinPoint.class);
        annotation = mock(TrackExecutionSla.class);

        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("VehicleService.createVehicle(..)");
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    @Test
    @DisplayName("Around Advice: викликає proceed(), повертає результат і фіксує метрику без порушення SLA")
    void measureExecutionTime_WithinSla_ShouldProceedAndRecordMetric() throws Throwable {
        when(annotation.operationName()).thenReturn("CREATE_VEHICLE");
        when(annotation.maxMillis()).thenReturn(500L);
        when(joinPoint.proceed()).thenReturn("CREATED_OK");

        Object result = aspect.measureExecutionTime(joinPoint, annotation);

        assertEquals("CREATED_OK", result);
        verify(joinPoint, times(1)).proceed();
        assertEquals(1, registry.getSlaMetrics().size());
        assertEquals("CREATE_VEHICLE", registry.getSlaMetrics().get(0).operation());
        assertFalse(registry.getSlaMetrics().get(0).slaViolated());
    }

    @Test
    @DisplayName("Around Advice: фіксує перевищення SLA, якщо виконання перевищило maxMillis")
    void measureExecutionTime_ExceedsSla_ShouldFlagViolation() throws Throwable {
        when(annotation.operationName()).thenReturn("");
        when(annotation.maxMillis()).thenReturn(5L);
        when(joinPoint.proceed()).thenAnswer(inv -> {
            Thread.sleep(15L);
            return "SLOW_RESULT";
        });

        Object result = aspect.measureExecutionTime(joinPoint, annotation);

        assertEquals("SLOW_RESULT", result);
        verify(joinPoint, times(1)).proceed();
        assertEquals(1, registry.getSlaMetrics().size());
        assertEquals("VehicleService.createVehicle(..)", registry.getSlaMetrics().get(0).operation());
        assertTrue(registry.getSlaMetrics().get(0).slaViolated());
    }

    @Test
    @DisplayName("Around Advice: прокидає виняток з proceed() і все одно записує час у finally")
    void measureExecutionTime_WhenProceedThrows_ShouldPropagateAndRecord() throws Throwable {
        when(annotation.operationName()).thenReturn("FAILING_OP");
        when(annotation.maxMillis()).thenReturn(200L);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("DB error"));

        assertThrows(IllegalStateException.class, () -> aspect.measureExecutionTime(joinPoint, annotation));

        verify(joinPoint, times(1)).proceed();
        assertEquals(1, registry.getSlaMetrics().size());
    }
}