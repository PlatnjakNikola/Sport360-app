package com.sport360.moduleservice.packages.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Read model over the v_package_summary view (per-package module counts + totals).
 * Mapped via @Subselect so it is never validated/created as a physical table.
 */
@Entity
@Immutable
@Subselect("select * from v_package_summary")
@Synchronize({"packages", "modules", "module_repairs", "package_statuses"})
public class VPackageSummary {

    @Id
    private Long id;

    @Column(name = "package_number")
    private String packageNumber;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "is_internal")
    private boolean internal;

    @Column(name = "status_code")
    private String statusCode;

    @Column(name = "status_name")
    private String statusName;

    @Column(name = "service_center_id")
    private Long serviceCenterId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @Column(name = "service_completed_at")
    private OffsetDateTime serviceCompletedAt;

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "arrived_at")
    private OffsetDateTime arrivedAt;

    @Column(name = "total_modules")
    private long totalModules;

    @Column(name = "repaired_count")
    private long repairedCount;

    @Column(name = "not_repairable_count")
    private long notRepairableCount;

    @Column(name = "total_value")
    private BigDecimal totalValue;

    protected VPackageSummary() {
    }

    public Long getId() {
        return id;
    }

    public String getPackageNumber() {
        return packageNumber;
    }

    public boolean isInternal() {
        return internal;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getStatusName() {
        return statusName;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public OffsetDateTime getShippedAt() {
        return shippedAt;
    }

    public long getTotalModules() {
        return totalModules;
    }

    public long getRepairedCount() {
        return repairedCount;
    }

    public long getNotRepairableCount() {
        return notRepairableCount;
    }

    public long getFinishedModules() {
        return repairedCount + notRepairableCount;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }
}
