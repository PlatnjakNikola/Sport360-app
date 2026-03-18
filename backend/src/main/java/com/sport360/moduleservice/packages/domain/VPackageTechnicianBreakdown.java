package com.sport360.moduleservice.packages.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.math.BigDecimal;

/**
 * Read model over v_package_technician_breakdown: per-package, per-technician repair totals.
 * The composite identity is unique per (package, technician); the technician id is the @Id.
 */
@Entity
@Immutable
@Subselect("select * from v_package_technician_breakdown")
@Synchronize({"modules", "module_repairs", "users"})
public class VPackageTechnicianBreakdown {

    @Id
    @Column(name = "assigned_technician_id")
    private Long technicianId;

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "technician_name")
    private String technicianName;

    @Column(name = "module_count")
    private long moduleCount;

    @Column(name = "repaired_count")
    private long repairedCount;

    @Column(name = "not_repairable_count")
    private long notRepairableCount;

    @Column(name = "total_pixels")
    private long totalPixels;

    @Column(name = "total_chips")
    private long totalChips;

    @Column(name = "total_value")
    private BigDecimal totalValue;

    protected VPackageTechnicianBreakdown() {
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public Long getPackageId() {
        return packageId;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public long getModuleCount() {
        return moduleCount;
    }

    public long getRepairedCount() {
        return repairedCount;
    }

    public long getNotRepairableCount() {
        return notRepairableCount;
    }

    public long getTotalPixels() {
        return totalPixels;
    }

    public long getTotalChips() {
        return totalChips;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }
}
