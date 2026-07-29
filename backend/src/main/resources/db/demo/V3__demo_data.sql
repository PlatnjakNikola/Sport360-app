-- Demo data for local development.
-- Admin is still created by AdminBootstrapRunner from application.yml:
-- admin@local.com / ChangeMe123!

WITH demo_users(role_code, name, email) AS (
    VALUES
        ('technician', 'Dominik Gelo', 'dominik.gelo@local.com'),
        ('technician', 'Nikola Platnjak', 'nikola.platnjak@local.com'),
        ('technician', 'Martina Svaco', 'martina.svaco@local.com'),
        ('client', 'TGI UK Contact', 'contact.uk@tgi.local'),
        ('client', 'TGI Barcelona Contact', 'contact.barcelona@tgi.local'),
        ('client', 'TGI Sansi Contact', 'contact.sansi@tgi.local')
)
INSERT INTO users (role_id, name, email, password_hash)
SELECT r.id, du.name, du.email, '$2a$10$ngd0SsbmK8vWu4RrO31.he9ZLrzEC6ibQg3AG8qWTIyilIDpqPUN.'
FROM demo_users du
JOIN roles r ON r.code = du.role_code
ON CONFLICT (email) DO NOTHING;

INSERT INTO technicians (user_id, phone, service_center_id)
SELECT u.id, v.phone, sc.id
FROM (
    VALUES
        ('dominik.gelo@local.com', '+385 91 111 2221'),
        ('nikola.platnjak@local.com', '+385 91 111 2222'),
        ('martina.svaco@local.com', '+385 91 111 2223')
) AS v(email, phone)
JOIN users u ON u.email = v.email
JOIN service_centers sc ON sc.code = 'ZAGREB'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO clients (user_id, company_name, contact_phone, address)
SELECT u.id, v.company_name, v.phone, v.address
FROM (
    VALUES
        ('contact.uk@tgi.local', 'TGI UK', '+44 20 0000 1001', '12 Demo Street, London, UK'),
        ('contact.barcelona@tgi.local', 'TGI BARCELONA', '+34 93 000 1002', '44 Demo Avenue, Barcelona, Spain'),
        ('contact.sansi@tgi.local', 'TGI SANSI', '+385 91 000 1003', '88 Demo Road, Zagreb, Croatia')
) AS v(email, company_name, phone, address)
JOIN users u ON u.email = v.email
ON CONFLICT (user_id) DO NOTHING;

WITH package_seed(package_number, client_email, status_code, description, note, approx_quantity, created_offset_days) AS (
    VALUES
        ('SANSI-26-001', 'contact.sansi@tgi.local', 'on_service', 'SANSI LED modules for service batch 001.', 'Mostly dead-pixel repairs expected.', 16, 18),
        ('SANSI-26-002', 'contact.sansi@tgi.local', 'repaired_waiting_shipment', 'SANSI LED modules for service batch 002.', 'Completed and waiting for shipment.', 14, 15),
        ('BARCELONA-26-001', 'contact.barcelona@tgi.local', 'received_by_service', 'Barcelona service pallet 001.', 'Package received and queued.', 12, 12),
        ('BARCELONA-26-002', 'contact.barcelona@tgi.local', 'shipped_to_client', 'Barcelona service pallet 002.', 'Repaired modules shipped back.', 18, 10),
        ('UK-26-001', 'contact.uk@tgi.local', 'sent_to_service', 'UK service package 001.', 'In transit to Zagreb service center.', 10, 8),
        ('UK-26-002', 'contact.uk@tgi.local', 'arrived', 'UK service package 002.', 'Client confirmed returned package arrival.', 15, 6)
)
INSERT INTO packages (
    package_number,
    client_id,
    is_internal,
    service_center_id,
    current_status_id,
    outbound_tracking_link,
    return_tracking_link,
    description,
    note,
    approx_quantity,
    created_by_user_id,
    received_at,
    service_started_at,
    service_completed_at,
    shipped_at,
    arrived_at,
    created_at,
    updated_at
)
SELECT
    ps.package_number,
    c.user_id,
    FALSE,
    sc.id,
    pkg_status.id,
    'https://tracking.local/out/' || lower(ps.package_number),
    CASE WHEN pkg_status.sort_order >= 6 THEN 'https://tracking.local/return/' || lower(ps.package_number) END,
    ps.description,
    ps.note,
    ps.approx_quantity,
    c.user_id,
    CASE WHEN pkg_status.sort_order >= 3 THEN NOW() - ((ps.created_offset_days - 2) || ' days')::interval END,
    CASE WHEN pkg_status.sort_order >= 4 THEN NOW() - ((ps.created_offset_days - 3) || ' days')::interval END,
    CASE WHEN pkg_status.sort_order >= 5 THEN NOW() - ((ps.created_offset_days - 5) || ' days')::interval END,
    CASE WHEN pkg_status.sort_order >= 6 THEN NOW() - ((ps.created_offset_days - 6) || ' days')::interval END,
    CASE WHEN pkg_status.sort_order >= 7 THEN NOW() - ((ps.created_offset_days - 7) || ' days')::interval END,
    NOW() - (ps.created_offset_days || ' days')::interval,
    NOW()
FROM package_seed ps
JOIN users u ON u.email = ps.client_email
JOIN clients c ON c.user_id = u.id
JOIN service_centers sc ON sc.code = 'ZAGREB'
JOIN package_statuses pkg_status ON pkg_status.code = ps.status_code
ON CONFLICT (package_number) WHERE deleted_at IS NULL DO NOTHING;

INSERT INTO package_status_history (package_id, status_id, changed_by_user_id, changed_at)
SELECT p.id, p.current_status_id, p.created_by_user_id, p.created_at
FROM packages p
WHERE p.package_number IN ('SANSI-26-001', 'SANSI-26-002', 'BARCELONA-26-001', 'BARCELONA-26-002', 'UK-26-001', 'UK-26-002')
  AND NOT EXISTS (
      SELECT 1 FROM package_status_history h WHERE h.package_id = p.id AND h.status_id = p.current_status_id
  );

WITH package_counts(package_number, module_count, number_base) AS (
    VALUES
        ('SANSI-26-001', 16, 344),
        ('SANSI-26-002', 14, 420),
        ('BARCELONA-26-001', 12, 510),
        ('BARCELONA-26-002', 18, 620),
        ('UK-26-001', 10, 730),
        ('UK-26-002', 15, 840)
),
module_seed AS (
    SELECT
        p.id AS package_id,
        p.current_status_id AS package_status_id,
        ps.code AS package_status_code,
        ps.sort_order AS package_status_sort,
        gs.n AS seq,
        'KC2400006R#1-2-240513-' || (pc.number_base + gs.n)::text AS module_number,
        CASE
            WHEN gs.n % 11 = 0 THEN 'black_screen'
            WHEN gs.n % 7 = 0 THEN 'image_stutter'
            WHEN gs.n % 9 = 0 THEN 'power_issues'
            WHEN gs.n % 13 = 0 THEN 'signal_issues'
            ELSE 'bright_line_dead_pixels'
        END AS problem_code,
        CASE
            WHEN ps.sort_order >= 5 AND gs.n % 10 = 0 THEN 'not_repairable'
            WHEN ps.sort_order >= 5 THEN 'repaired'
            WHEN ps.code = 'on_service' AND gs.n % 4 <> 0 THEN 'repaired'
            ELSE 'waiting_for_repair'
        END AS module_status_code,
        CASE gs.n % 3
            WHEN 1 THEN 'dominik.gelo@local.com'
            WHEN 2 THEN 'nikola.platnjak@local.com'
            ELSE 'martina.svaco@local.com'
        END AS technician_email
    FROM package_counts pc
    JOIN packages p ON p.package_number = pc.package_number
    JOIN package_statuses ps ON ps.id = p.current_status_id
    CROSS JOIN LATERAL generate_series(1, pc.module_count) AS gs(n)
)
INSERT INTO modules (
    package_id,
    module_number,
    problem_type_id,
    current_status_id,
    assigned_technician_id,
    created_by_user_id,
    created_at,
    updated_at
)
SELECT
    ms.package_id,
    ms.module_number,
    pt.id,
    mstat.id,
    tech.id,
    tech.id,
    NOW() - ((20 - ms.seq) || ' hours')::interval,
    NOW()
FROM module_seed ms
JOIN problem_types pt ON pt.code = ms.problem_code
JOIN module_statuses mstat ON mstat.code = ms.module_status_code
JOIN users tech ON tech.email = ms.technician_email
ON CONFLICT (package_id, module_number) WHERE deleted_at IS NULL DO NOTHING;

INSERT INTO module_status_history (module_id, status_id, changed_by_user_id, changed_at)
SELECT m.id, m.current_status_id, m.created_by_user_id, m.created_at
FROM modules m
JOIN packages p ON p.id = m.package_id
WHERE p.package_number IN ('SANSI-26-001', 'SANSI-26-002', 'BARCELONA-26-001', 'BARCELONA-26-002', 'UK-26-001', 'UK-26-002')
  AND NOT EXISTS (
      SELECT 1 FROM module_status_history h WHERE h.module_id = m.id AND h.status_id = m.current_status_id
  );

WITH repair_source AS (
    SELECT
        m.id AS module_id,
        m.assigned_technician_id,
        ms.code AS status_code,
        pt.code AS problem_code,
        row_number() OVER (PARTITION BY m.package_id ORDER BY m.module_number) AS seq
    FROM modules m
    JOIN packages p ON p.id = m.package_id
    JOIN module_statuses ms ON ms.id = m.current_status_id
    JOIN problem_types pt ON pt.id = m.problem_type_id
    WHERE p.package_number IN ('SANSI-26-001', 'SANSI-26-002', 'BARCELONA-26-001', 'BARCELONA-26-002', 'UK-26-001', 'UK-26-002')
      AND ms.code IN ('repaired', 'not_repairable')
),
repair_values AS (
    SELECT
        rs.*,
        CASE
            WHEN rs.status_code = 'not_repairable' THEN 0
            WHEN rs.problem_code = 'bright_line_dead_pixels' THEN LEAST(30, 2 + (rs.seq * 3) % 29)
            ELSE 0
        END AS pixels_repaired,
        CASE
            WHEN rs.status_code = 'not_repairable' THEN 0
            WHEN rs.problem_code IN ('black_screen', 'image_stutter', 'power_issues', 'signal_issues') THEN 1 + (rs.seq % 6)
            ELSE 0
        END AS chips_replaced
    FROM repair_source rs
)
INSERT INTO module_repairs (
    module_id,
    technician_id,
    decision_status_id,
    pixels_repaired,
    chips_replaced,
    repair_note,
    price,
    completed_at
)
SELECT
    rv.module_id,
    rv.assigned_technician_id,
    ms.id,
    rv.pixels_repaired,
    rv.chips_replaced,
    CASE
        WHEN rv.status_code = 'not_repairable' THEN 'Module inspected and marked not repairable because the panel fault was too severe for economical service.'
        WHEN rv.problem_code = 'bright_line_dead_pixels' THEN 'Replaced faulty LEDs and corrected the bright line/dead pixel area. Final visual test passed.'
        WHEN rv.problem_code = 'black_screen' THEN 'Replaced failed IC components and restored image output. Power and signal tests passed.'
        WHEN rv.problem_code = 'image_stutter' THEN 'Replaced unstable IC components and verified stable playback under test signal.'
        WHEN rv.problem_code = 'power_issues' THEN 'Repaired power-related IC fault and verified normal startup behavior.'
        WHEN rv.problem_code = 'signal_issues' THEN 'Repaired signal path components and confirmed stable input detection.'
        ELSE 'Completed standard module service and verification.'
    END,
    LEAST(
        150.00,
        CASE
            WHEN rv.status_code = 'not_repairable' THEN 0.00
            WHEN rv.problem_code = 'bright_line_dead_pixels' THEN 20.00 + (rv.pixels_repaired * 2.50)
            WHEN rv.problem_code = 'black_screen' THEN 45.00 + (rv.chips_replaced * 12.00)
            WHEN rv.problem_code = 'image_stutter' THEN 35.00 + (rv.chips_replaced * 10.00)
            WHEN rv.problem_code = 'power_issues' THEN 40.00 + (rv.chips_replaced * 11.00)
            WHEN rv.problem_code = 'signal_issues' THEN 30.00 + (rv.chips_replaced * 9.00)
            ELSE 25.00
        END
    ),
    NOW() - ((12 - rv.seq % 10) || ' hours')::interval
FROM repair_values rv
JOIN module_statuses ms ON ms.code = rv.status_code
ON CONFLICT (module_id) DO NOTHING;

INSERT INTO audit_logs (entity_type, entity_id, action_type, changed_by_user_id, created_at)
SELECT 'module', m.id, 'create', m.created_by_user_id, m.created_at
FROM modules m
JOIN packages p ON p.id = m.package_id
WHERE p.package_number IN ('SANSI-26-001', 'SANSI-26-002', 'BARCELONA-26-001', 'BARCELONA-26-002', 'UK-26-001', 'UK-26-002')
  AND NOT EXISTS (
      SELECT 1 FROM audit_logs a WHERE a.entity_type = 'module' AND a.entity_id = m.id AND a.action_type = 'create'
  );

INSERT INTO audit_logs (entity_type, entity_id, action_type, changed_by_user_id, created_at)
SELECT 'repair', mr.module_id, 'create', mr.technician_id, mr.completed_at
FROM module_repairs mr
JOIN modules m ON m.id = mr.module_id
JOIN packages p ON p.id = m.package_id
WHERE p.package_number IN ('SANSI-26-001', 'SANSI-26-002', 'BARCELONA-26-001', 'BARCELONA-26-002', 'UK-26-001', 'UK-26-002')
  AND NOT EXISTS (
      SELECT 1 FROM audit_logs a WHERE a.entity_type = 'repair' AND a.entity_id = mr.module_id AND a.action_type = 'create'
  );
