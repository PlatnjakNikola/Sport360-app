package com.sport360.moduleservice.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fixed system role (admin / technician / client). Seeded, read-only at runtime. */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    protected Role() {
    }

    public Short getId() {
        return id;
    }

    public String getCode() {
        return code;
    }
}
