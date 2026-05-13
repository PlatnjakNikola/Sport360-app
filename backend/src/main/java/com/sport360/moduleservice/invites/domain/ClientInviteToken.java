package com.sport360.moduleservice.invites.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** Admin-created invite for a client account (SHA-256 hashed token). */
@Entity
@Table(name = "client_invite_tokens")
public class ClientInviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "contact_name", nullable = false, length = 150)
    private String contactName;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ClientInviteToken() {
    }

    public ClientInviteToken(Long createdByUserId, String email, String contactName, String companyName,
                             String contactPhone, String address, String tokenHash, OffsetDateTime expiresAt) {
        this.createdByUserId = createdByUserId;
        this.email = email;
        this.contactName = contactName;
        this.companyName = companyName;
        this.contactPhone = contactPhone;
        this.address = address;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getContactName() {
        return contactName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getAddress() {
        return address;
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
