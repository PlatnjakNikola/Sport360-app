-- V1 — initial schema for the Module Service app.
-- Faithful to docs/database-schema.md (tables, indexes, views, lookup seed),
-- plus the owner-approved hardening: is_internal/client_id CHECK and partial
-- unique indexes that ignore soft-deleted rows.
--
-- The admin account is intentionally NOT seeded here. It is created on first
-- start by a Spring ApplicationRunner from APP_BOOTSTRAP_ADMIN_* env vars.

-- =====================================================================
-- Lookup tables (seeded, mostly static)
-- =====================================================================

CREATE TABLE roles (
    id   SMALLINT     PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE
);

CREATE TABLE service_centers (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    country    VARCHAR(100),
    city       VARCHAR(100),
    address    TEXT,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE package_statuses (
    id         SMALLINT     PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    sort_order SMALLINT     NOT NULL UNIQUE
);

CREATE TABLE problem_types (
    id         SMALLINT     PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    sort_order SMALLINT     NOT NULL UNIQUE,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE module_statuses (
    id         SMALLINT     PRIMARY KEY,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    sort_order SMALLINT     NOT NULL UNIQUE,
    is_final   BOOLEAN      NOT NULL
);

-- =====================================================================
-- Users and profiles
-- =====================================================================

CREATE TABLE users (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id       SMALLINT     NOT NULL REFERENCES roles (id),
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(72)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE clients (
    user_id       BIGINT       PRIMARY KEY REFERENCES users (id),
    company_name  VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    address       TEXT
);

CREATE TABLE technicians (
    user_id           BIGINT      PRIMARY KEY REFERENCES users (id),
    phone             VARCHAR(50),
    service_center_id BIGINT      NOT NULL REFERENCES service_centers (id)
);

-- =====================================================================
-- Packages and workflow
-- =====================================================================

CREATE TABLE packages (
    id                     BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    package_number         VARCHAR(100)  NOT NULL,
    client_id              BIGINT        REFERENCES clients (user_id),
    is_internal            BOOLEAN       NOT NULL DEFAULT FALSE,
    service_center_id      BIGINT        NOT NULL REFERENCES service_centers (id),
    current_status_id      SMALLINT      NOT NULL DEFAULT 1 REFERENCES package_statuses (id),
    outbound_tracking_link VARCHAR(2048),
    return_tracking_link   VARCHAR(2048),
    description            TEXT,
    note                   TEXT,
    approx_quantity        INT           CHECK (approx_quantity >= 0),
    created_by_user_id     BIGINT        NOT NULL REFERENCES users (id),
    received_at            TIMESTAMPTZ,
    service_started_at     TIMESTAMPTZ,
    service_completed_at   TIMESTAMPTZ,
    shipped_at             TIMESTAMPTZ,
    arrived_at             TIMESTAMPTZ,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at             TIMESTAMPTZ,
    CONSTRAINT chk_packages_internal_client CHECK (
        (is_internal AND client_id IS NULL)
        OR (NOT is_internal AND client_id IS NOT NULL)
    )
);

CREATE TABLE package_status_history (
    id                 BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    package_id         BIGINT      NOT NULL REFERENCES packages (id),
    status_id          SMALLINT    NOT NULL REFERENCES package_statuses (id),
    changed_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    changed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE modules (
    id                     BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    package_id             BIGINT       NOT NULL REFERENCES packages (id),
    module_number          VARCHAR(150) NOT NULL,
    problem_type_id        SMALLINT     NOT NULL REFERENCES problem_types (id),
    current_status_id      SMALLINT     NOT NULL DEFAULT 1 REFERENCES module_statuses (id),
    assigned_technician_id BIGINT       NOT NULL REFERENCES technicians (user_id),
    created_by_user_id     BIGINT       NOT NULL REFERENCES users (id),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at             TIMESTAMPTZ
);

CREATE TABLE module_status_history (
    id                 BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    module_id          BIGINT      NOT NULL REFERENCES modules (id),
    status_id          SMALLINT    NOT NULL REFERENCES module_statuses (id),
    changed_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    changed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE module_repairs (
    module_id          BIGINT        PRIMARY KEY REFERENCES modules (id),
    technician_id      BIGINT        NOT NULL REFERENCES technicians (user_id),
    decision_status_id SMALLINT      NOT NULL REFERENCES module_statuses (id),
    pixels_repaired    SMALLINT      NOT NULL DEFAULT 0 CHECK (pixels_repaired >= 0),
    chips_replaced     SMALLINT      NOT NULL DEFAULT 0 CHECK (chips_replaced >= 0),
    repair_note        TEXT,
    price              DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (price >= 0),
    completed_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE module_images (
    id                  BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    module_id           BIGINT        NOT NULL REFERENCES modules (id),
    file_path           VARCHAR(2048) NOT NULL,
    uploaded_by_user_id BIGINT        NOT NULL REFERENCES users (id),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- Auth, audit and notifications
-- =====================================================================

CREATE TABLE audit_logs (
    id                 BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type        VARCHAR(50) NOT NULL,
    entity_id          BIGINT      NOT NULL,
    action_type        VARCHAR(50) NOT NULL,
    changed_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    old_value_json     JSONB,
    new_value_json     JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ
);

CREATE TABLE password_reset_tokens (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE technician_invite_tokens (
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_by_user_id BIGINT       NOT NULL REFERENCES users (id),
    email              VARCHAR(255) NOT NULL,
    name               VARCHAR(150) NOT NULL,
    service_center_id  BIGINT       NOT NULL REFERENCES service_centers (id),
    phone              VARCHAR(50),
    token_hash         VARCHAR(255) NOT NULL UNIQUE,
    expires_at         TIMESTAMPTZ  NOT NULL,
    used_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE client_invite_tokens (
    id                 BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_by_user_id BIGINT       NOT NULL REFERENCES users (id),
    email              VARCHAR(255) NOT NULL,
    contact_name       VARCHAR(150) NOT NULL,
    company_name       VARCHAR(255) NOT NULL,
    contact_phone      VARCHAR(50),
    address            TEXT,
    token_hash         VARCHAR(255) NOT NULL UNIQUE,
    expires_at         TIMESTAMPTZ  NOT NULL,
    used_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE mfa_codes (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    code_hash  VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    attempts   SMALLINT     NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id),
    type        VARCHAR(50)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    message     TEXT,
    entity_type VARCHAR(50),
    entity_id   BIGINT,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- =====================================================================
-- Indexes
-- =====================================================================

-- Standard
CREATE INDEX idx_packages_client_id     ON packages (client_id);
CREATE INDEX idx_packages_status        ON packages (current_status_id);
CREATE INDEX idx_packages_service_center ON packages (service_center_id);
CREATE INDEX idx_modules_package_id     ON modules (package_id);
CREATE INDEX idx_modules_technician     ON modules (assigned_technician_id);
CREATE INDEX idx_modules_number         ON modules (module_number);
CREATE INDEX idx_cit_email              ON client_invite_tokens (email);
CREATE INDEX idx_mfa_user               ON mfa_codes (user_id);
CREATE INDEX idx_audit_entity           ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_user             ON audit_logs (changed_by_user_id);
CREATE INDEX idx_audit_created          ON audit_logs (created_at);
CREATE INDEX idx_psh_package            ON package_status_history (package_id);
CREATE INDEX idx_msh_module             ON module_status_history (module_id);
CREATE INDEX idx_mi_module              ON module_images (module_id);
CREATE INDEX idx_audit_action_type      ON audit_logs (action_type);

-- Composite (active rows only)
CREATE INDEX idx_packages_client_status ON packages (client_id, current_status_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_packages_sc_status     ON packages (service_center_id, current_status_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_modules_package_status ON modules (package_id, current_status_id) WHERE deleted_at IS NULL;

-- Partial
CREATE INDEX idx_packages_active          ON packages (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_modules_active           ON modules (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_modules_waiting          ON modules (package_id) WHERE current_status_id = 1;
CREATE INDEX idx_notif_unread             ON notifications (user_id, created_at DESC) WHERE is_read = FALSE;
CREATE INDEX idx_rt_user_active           ON refresh_tokens (user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_notif_user_created       ON notifications (user_id, created_at DESC);
CREATE INDEX idx_packages_arrived_cleanup ON packages (arrived_at) WHERE arrived_at IS NOT NULL AND deleted_at IS NULL;

-- Partial unique (uniqueness among live rows; a soft-deleted number is freed)
CREATE UNIQUE INDEX uq_packages_number_active   ON packages (package_number) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_modules_pkg_number_active ON modules (package_id, module_number) WHERE deleted_at IS NULL;

-- =====================================================================
-- Views (read models — mapped as @Immutable JPA entities)
-- =====================================================================

CREATE VIEW v_package_summary AS
SELECT
    p.id, p.package_number, p.client_id, p.is_internal,
    p.current_status_id, ps.code AS status_code, ps.name AS status_name,
    p.service_center_id, p.created_at,
    p.received_at, p.service_completed_at, p.shipped_at, p.arrived_at,
    COUNT(m.id) AS total_modules,
    COUNT(m.id) FILTER (WHERE ms.code = 'repaired') AS repaired_count,
    COUNT(m.id) FILTER (WHERE ms.code = 'not_repairable') AS not_repairable_count,
    COALESCE(SUM(mr.price), 0) AS total_value
FROM packages p
JOIN package_statuses ps ON p.current_status_id = ps.id
LEFT JOIN modules m ON m.package_id = p.id AND m.deleted_at IS NULL
LEFT JOIN module_statuses ms ON m.current_status_id = ms.id
LEFT JOIN module_repairs mr ON mr.module_id = m.id
WHERE p.deleted_at IS NULL
GROUP BY p.id, ps.code, ps.name;

CREATE VIEW v_package_technician_breakdown AS
SELECT
    m.package_id,
    m.assigned_technician_id,
    u.name AS technician_name,
    COUNT(m.id) AS module_count,
    COUNT(m.id) FILTER (WHERE ms.code = 'repaired') AS repaired_count,
    COUNT(m.id) FILTER (WHERE ms.code = 'not_repairable') AS not_repairable_count,
    COALESCE(SUM(mr.pixels_repaired), 0) AS total_pixels,
    COALESCE(SUM(mr.chips_replaced), 0) AS total_chips,
    COALESCE(SUM(mr.price), 0) AS total_value
FROM modules m
JOIN users u ON m.assigned_technician_id = u.id
LEFT JOIN module_statuses ms ON m.current_status_id = ms.id
LEFT JOIN module_repairs mr ON mr.module_id = m.id
WHERE m.deleted_at IS NULL
GROUP BY m.package_id, m.assigned_technician_id, u.name;

CREATE VIEW v_module_detail AS
SELECT
    m.id, m.package_id, m.module_number,
    m.current_status_id, ms.code AS status_code, ms.name AS status_name,
    m.assigned_technician_id, u.name AS technician_name,
    m.problem_type_id, pt.name AS problem_type_name,
    mr.pixels_repaired, mr.chips_replaced,
    mr.repair_note, mr.price, mr.completed_at,
    mr.decision_status_id,
    m.created_at
FROM modules m
JOIN module_statuses ms ON m.current_status_id = ms.id
JOIN problem_types pt ON m.problem_type_id = pt.id
LEFT JOIN users u ON m.assigned_technician_id = u.id
LEFT JOIN module_repairs mr ON mr.module_id = m.id
WHERE m.deleted_at IS NULL;

-- =====================================================================
-- Seed data (lookup tables only)
-- =====================================================================

INSERT INTO roles (id, code) VALUES
    (1, 'admin'),
    (2, 'technician'),
    (3, 'client');

INSERT INTO package_statuses (id, code, name, sort_order) VALUES
    (1, 'created',                   'Created',                     1),
    (2, 'sent_to_service',           'Sent to service',             2),
    (3, 'received_by_service',       'Received by service',         3),
    (4, 'on_service',                'On service',                  4),
    (5, 'repaired_waiting_shipment', 'Repaired — waiting shipment',  5),
    (6, 'shipped_to_client',         'Shipped to client',           6),
    (7, 'arrived',                   'Arrived',                     7);

INSERT INTO module_statuses (id, code, name, sort_order, is_final) VALUES
    (1, 'waiting_for_repair', 'Waiting for repair', 1, FALSE),
    (2, 'repaired',           'Repaired',           2, TRUE),
    (3, 'not_repairable',     'Not repairable',     3, TRUE);

INSERT INTO problem_types (id, code, name, sort_order, is_active) VALUES
    (1,  'black_screen',            'Black screen',                                   1,  TRUE),
    (2,  'blue_screen',             'Blue screen',                                    2,  TRUE),
    (3,  'garbled_screen',          'Garbled screen',                                 3,  TRUE),
    (4,  'flickering',              'Flickering',                                     4,  TRUE),
    (5,  'color_loss',              'Partial area color loss or color difference',    5,  TRUE),
    (6,  'bright_line_dead_pixels', 'Bright line or dead pixels',                     6,  TRUE),
    (7,  'image_stutter',           'Image stuttering or delay',                      7,  TRUE),
    (8,  'display_misalignment',    'Display misalignment',                           8,  TRUE),
    (9,  'resolution_offset',       'Resolution, offset or rotation issues',          9,  TRUE),
    (10, 'signal_issues',           'Signal issues',                                  10, TRUE),
    (11, 'brightness_sensor',       'Brightness sensor issues',                       11, TRUE),
    (12, 'power_issues',            'Power issues',                                   12, TRUE),
    (13, 'other',                   'Other',                                          13, TRUE);

INSERT INTO service_centers (code, name, country, city) VALUES
    ('ZAGREB', 'Zagreb', 'Croatia', 'Zagreb');
