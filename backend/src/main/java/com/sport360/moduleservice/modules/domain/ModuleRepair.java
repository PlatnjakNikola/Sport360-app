package com.sport360.moduleservice.modules.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Final repair outcome for a module. 1:1 with the module (shared PK). No re-repair. */
@Entity
@Table(name = "module_repairs")
public class ModuleRepair {

    @Id
    @Column(name = "module_id")
    private Long moduleId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "module_id")
    private Module module;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "decision_status_id", nullable = false)
    private ModuleStatus decisionStatus;

    @Column(name = "pixels_repaired", nullable = false)
    private short pixelsRepaired;

    @Column(name = "chips_replaced", nullable = false)
    private short chipsReplaced;

    @Column(name = "repair_note", columnDefinition = "text")
    private String repairNote;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "completed_at", nullable = false, updatable = false)
    private OffsetDateTime completedAt;

    protected ModuleRepair() {
    }

    public ModuleRepair(Module module, Long technicianId, ModuleStatus decisionStatus, short pixelsRepaired,
                        short chipsReplaced, String repairNote, BigDecimal price) {
        this.module = module;
        this.technicianId = technicianId;
        this.decisionStatus = decisionStatus;
        this.pixelsRepaired = pixelsRepaired;
        this.chipsReplaced = chipsReplaced;
        this.repairNote = repairNote;
        this.price = price;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public ModuleStatus getDecisionStatus() {
        return decisionStatus;
    }

    public void setDecisionStatus(ModuleStatus decisionStatus) {
        this.decisionStatus = decisionStatus;
    }

    public short getPixelsRepaired() {
        return pixelsRepaired;
    }

    public void setPixelsRepaired(short pixelsRepaired) {
        this.pixelsRepaired = pixelsRepaired;
    }

    public short getChipsReplaced() {
        return chipsReplaced;
    }

    public void setChipsReplaced(short chipsReplaced) {
        this.chipsReplaced = chipsReplaced;
    }

    public String getRepairNote() {
        return repairNote;
    }

    public void setRepairNote(String repairNote) {
        this.repairNote = repairNote;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
}
