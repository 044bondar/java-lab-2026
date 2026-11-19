package com.vagcv.garage.aop;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DealAuditRegistry {

    public record AuditEntry(
            String action,
            Long vehicleId,
            String vin,
            String operator,
            LocalDateTime timestamp
    ) {}

    public record SlaMetric(
            String operation,
            long durationMs,
            boolean slaViolated
    ) {}

    private final List<AuditEntry> auditEntries = new CopyOnWriteArrayList<>();
    private final List<SlaMetric> slaMetrics = new CopyOnWriteArrayList<>();

    public void recordAudit(AuditEntry entry) {
        auditEntries.add(entry);
    }

    public void recordSla(SlaMetric metric) {
        slaMetrics.add(metric);
    }

    public List<AuditEntry> getAuditEntries() {
        return List.copyOf(auditEntries);
    }

    public List<SlaMetric> getSlaMetrics() {
        return List.copyOf(slaMetrics);
    }

    public void clear() {
        auditEntries.clear();
        slaMetrics.clear();
    }
}