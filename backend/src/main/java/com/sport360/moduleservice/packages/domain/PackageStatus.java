package com.sport360.moduleservice.packages.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Lookup of package workflow statuses. Seeded, read-only at runtime. */
@Entity
@Table(name = "package_statuses")
public class PackageStatus {

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    protected PackageStatus() {
    }

    public Short getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Short getSortOrder() {
        return sortOrder;
    }
}
