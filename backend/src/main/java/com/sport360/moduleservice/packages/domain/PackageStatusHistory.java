package com.sport360.moduleservice.packages.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** One row per package status change. */
@Entity
@Table(name = "package_status_history")
public class PackageStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "status_id", nullable = false)
    private Short statusId;

    @Column(name = "changed_by_user_id", nullable = false)
    private Long changedByUserId;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    protected PackageStatusHistory() {
    }

    public PackageStatusHistory(Long packageId, Short statusId, Long changedByUserId) {
        this.packageId = packageId;
        this.statusId = statusId;
        this.changedByUserId = changedByUserId;
    }

    public Long getId() {
        return id;
    }
}
