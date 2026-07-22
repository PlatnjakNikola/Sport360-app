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
 * Admin read model over v_admin_package_summary: like {@link VPackageSummary} but includes
 * soft-deleted packages and the client company name, for the admin "All packages" view + Trash.
 */
@Entity
@Immutable
@Subselect("select * from v_admin_package_summary")
@Synchronize({"packages", "modules", "module_repairs", "package_statuses", "clients"})
public class VAdminPackageSummary {

    @Id
    private Long id;

    @Column(name = "package_number")
    private String packageNumber;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "company_name")
    private String companyName;

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

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "total_modules")
    private long totalModules;

    @Column(name = "repaired_count")
    private long repairedCount;

    @Column(name = "not_repairable_count")
    private long notRepairableCount;

    @Column(name = "total_value")
    private BigDecimal totalValue;

    protected VAdminPackageSummary() {
    }

    public Long getId() {
        return id;
    }

    public String getPackageNumber() {
        return packageNumber;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getCompanyName() {
        return companyName;
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

    public Long getServiceCenterId() {
        return serviceCenterId;
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

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
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

    public BigDecimal getTotalValue() {
        return totalValue;
    }
}
