package com.sport360.moduleservice.invites.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** Admin-created invite for a technician account (SHA-256 hashed token). */
@Entity
@Table(name = "technician_invite_tokens")
public class TechnicianInviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "service_center_id", nullable = false)
    private Long serviceCenterId;

    @Column(length = 50)
    private String phone;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected TechnicianInviteToken() {
    }

    public TechnicianInviteToken(Long createdByUserId, String email, String name, Long serviceCenterId,
                                 String phone, String tokenHash, OffsetDateTime expiresAt) {
        this.createdByUserId = createdByUserId;
        this.email = email;
        this.name = name;
        this.serviceCenterId = serviceCenterId;
        this.phone = phone;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public Long getServiceCenterId() {
        return serviceCenterId;
    }

    public String getPhone() {
        return phone;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void markUsed(OffsetDateTime when) {
        this.usedAt = when;
    }

    public void regenerate(String tokenHash, OffsetDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(OffsetDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
