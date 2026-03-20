package com.sport360.moduleservice.packages.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/** Package / shipment. External (client) packages have a client; internal ones do not. */
@Entity
@Table(name = "packages")
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_number", nullable = false, length = 100)
    private String packageNumber;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "is_internal", nullable = false)
    private boolean internal = false;

    @Column(name = "service_center_id", nullable = false)
    private Long serviceCenterId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "current_status_id", nullable = false)
    private PackageStatus currentStatus;

    @Column(name = "outbound_tracking_link", length = 2048)
    private String outboundTrackingLink;

    @Column(name = "return_tracking_link", length = 2048)
    private String returnTrackingLink;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "approx_quantity")
    private Integer approxQuantity;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @Column(name = "service_started_at")
    private OffsetDateTime serviceStartedAt;

    @Column(name = "service_completed_at")
    private OffsetDateTime serviceCompletedAt;

    @Column(name = "shipped_at")
    private OffsetDateTime shippedAt;

    @Column(name = "arrived_at")
    private OffsetDateTime arrivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Package() {
    }

    /** Creates an external (client) package in the initial 'created' status. */
    public Package(String packageNumber, Long clientId, Long serviceCenterId, PackageStatus currentStatus,
                   Long createdByUserId, String outboundTrackingLink, String description, String note,
                   Integer approxQuantity) {
        this.packageNumber = packageNumber;
        this.clientId = clientId;
        this.internal = false;
        this.serviceCenterId = serviceCenterId;
        this.currentStatus = currentStatus;
        this.createdByUserId = createdByUserId;
        this.outboundTrackingLink = outboundTrackingLink;
        this.description = description;
        this.note = note;
        this.approxQuantity = approxQuantity;
    }

    /** Creates an internal (Sport360) package: no client, the label is the package number. */
    public static Package internal(String packageNumber, Long serviceCenterId, PackageStatus currentStatus,
                                   Long createdByUserId, String description, String note, Integer approxQuantity) {
        Package pkg = new Package();
        pkg.packageNumber = packageNumber;
        pkg.clientId = null;
        pkg.internal = true;
        pkg.serviceCenterId = serviceCenterId;
        pkg.currentStatus = currentStatus;
        pkg.createdByUserId = createdByUserId;
        pkg.description = description;
        pkg.note = note;
        pkg.approxQuantity = approxQuantity;
        return pkg;
    }

    public Long getId() {
        return id;
    }

    public String getPackageNumber() {
        return packageNumber;
    }

    public void setPackageNumber(String packageNumber) {
        this.packageNumber = packageNumber;
    }

    public Long getClientId() {
        return clientId;
    }

    public boolean isInternal() {
        return internal;
    }

    public PackageStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(PackageStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getOutboundTrackingLink() {
        return outboundTrackingLink;
    }

    public void setOutboundTrackingLink(String outboundTrackingLink) {
        this.outboundTrackingLink = outboundTrackingLink;
    }

    public Long getServiceCenterId() {
        return serviceCenterId;
    }

    public String getReturnTrackingLink() {
        return returnTrackingLink;
    }

    public void setReturnTrackingLink(String returnTrackingLink) {
        this.returnTrackingLink = returnTrackingLink;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getApproxQuantity() {
        return approxQuantity;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(OffsetDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public OffsetDateTime getServiceStartedAt() {
        return serviceStartedAt;
    }

    public void setServiceStartedAt(OffsetDateTime serviceStartedAt) {
        this.serviceStartedAt = serviceStartedAt;
    }

    public OffsetDateTime getServiceCompletedAt() {
        return serviceCompletedAt;
    }

    public void setServiceCompletedAt(OffsetDateTime serviceCompletedAt) {
        this.serviceCompletedAt = serviceCompletedAt;
    }

    public OffsetDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(OffsetDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public OffsetDateTime getArrivedAt() {
        return arrivedAt;
    }

    public void setArrivedAt(OffsetDateTime arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
