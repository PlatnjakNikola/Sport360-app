package com.sport360.moduleservice.problemtypes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Lookup of module problem types. Seeded; admin can add/deactivate (Phase 8). */
@Entity
@Table(name = "problem_types")
public class ProblemType {

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected ProblemType() {
    }

    public ProblemType(Short id, String code, String name, Short sortOrder) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = true;
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

    public void setName(String name) {
        this.name = name;
    }

    public Short getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Short sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
