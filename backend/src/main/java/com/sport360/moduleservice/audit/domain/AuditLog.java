package com.sport360.moduleservice.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** Critical system change log. Values are stored as JSONB (never secrets). */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "changed_by_user_id", nullable = false)
    private Long changedByUserId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value_json")
    private String oldValueJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value_json")
    private String newValueJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(String entityType, Long entityId, String actionType, Long changedByUserId,
                    String oldValueJson, String newValueJson) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.actionType = actionType;
        this.changedByUserId = changedByUserId;
        this.oldValueJson = oldValueJson;
        this.newValueJson = newValueJson;
    }

    public Long getId() {
        return id;
    }
}
