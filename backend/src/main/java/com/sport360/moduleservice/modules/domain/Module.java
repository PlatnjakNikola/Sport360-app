package com.sport360.moduleservice.modules.domain;

import com.sport360.moduleservice.problemtypes.domain.ProblemType;
import com.sport360.moduleservice.technicians.domain.Technician;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/** A module created in service (scanned from a package), assigned to a technician. */
@Entity
@Table(name = "modules")
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "module_number", nullable = false, length = 150)
    private String moduleNumber;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "problem_type_id", nullable = false)
    private ProblemType problemType;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "current_status_id", nullable = false)
    private ModuleStatus currentStatus;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_technician_id", nullable = false)
    private Technician assignedTechnician;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @OneToOne(mappedBy = "module", fetch = FetchType.LAZY)
    private ModuleRepair repair;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Module() {
    }

    public Module(Long packageId, String moduleNumber, ProblemType problemType, ModuleStatus currentStatus,
                  Technician assignedTechnician, Long createdByUserId) {
        this.packageId = packageId;
        this.moduleNumber = moduleNumber;
        this.problemType = problemType;
        this.currentStatus = currentStatus;
        this.assignedTechnician = assignedTechnician;
        this.createdByUserId = createdByUserId;
    }

    public Long getId() {
        return id;
    }

    public Long getPackageId() {
        return packageId;
    }

    public String getModuleNumber() {
        return moduleNumber;
    }

    public ProblemType getProblemType() {
        return problemType;
    }

    public void setProblemType(ProblemType problemType) {
        this.problemType = problemType;
    }

    public ModuleStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ModuleStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Technician getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(Technician assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public ModuleRepair getRepair() {
        return repair;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
