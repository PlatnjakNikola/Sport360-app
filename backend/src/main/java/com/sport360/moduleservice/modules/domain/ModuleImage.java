package com.sport360.moduleservice.modules.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** A stored image for a module. The DB holds only the storage path; bytes live behind ImageStorageService. */
@Entity
@Table(name = "module_images")
public class ModuleImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    @Column(name = "file_path", nullable = false, length = 2048)
    private String filePath;

    @Column(name = "uploaded_by_user_id", nullable = false)
    private Long uploadedByUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ModuleImage() {
    }

    public ModuleImage(Long moduleId, String filePath, Long uploadedByUserId) {
        this.moduleId = moduleId;
        this.filePath = filePath;
        this.uploadedByUserId = uploadedByUserId;
    }

    public Long getId() {
        return id;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public String getFilePath() {
        return filePath;
    }

    public Long getUploadedByUserId() {
        return uploadedByUserId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
