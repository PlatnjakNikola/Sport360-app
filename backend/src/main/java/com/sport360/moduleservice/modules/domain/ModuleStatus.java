package com.sport360.moduleservice.modules.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Lookup of module statuses (waiting_for_repair / repaired / not_repairable). */
@Entity
@Table(name = "module_statuses")
public class ModuleStatus {

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;

    @Column(name = "is_final", nullable = false)
    private boolean finalStatus;

    protected ModuleStatus() {
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

    public boolean isFinalStatus() {
        return finalStatus;
    }
}
