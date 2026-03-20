package com.sport360.moduleservice.modules.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** One row per module status change. */
@Entity
@Table(name = "module_status_history")
public class ModuleStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    @Column(name = "status_id", nullable = false)
    private Short statusId;

    @Column(name = "changed_by_user_id", nullable = false)
    private Long changedByUserId;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    protected ModuleStatusHistory() {
    }

    public ModuleStatusHistory(Long moduleId, Short statusId, Long changedByUserId) {
        this.moduleId = moduleId;
        this.statusId = statusId;
        this.changedByUserId = changedByUserId;
    }

    public Long getId() {
        return id;
    }
}
