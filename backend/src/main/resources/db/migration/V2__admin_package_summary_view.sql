-- =====================================================================
-- Admin package summary
-- Mirrors v_package_summary but WITHOUT the deleted-at filter, so the admin
-- "All packages" view and Trash can see soft-deleted rows. Adds the client
-- company name and deleted_at for the admin list/filters.
-- =====================================================================

CREATE VIEW v_admin_package_summary AS
SELECT
    p.id, p.package_number, p.client_id, c.company_name, p.is_internal,
    p.current_status_id, ps.code AS status_code, ps.name AS status_name,
    p.service_center_id, p.created_at,
    p.received_at, p.service_started_at, p.service_completed_at, p.shipped_at, p.arrived_at,
    p.deleted_at,
    COUNT(m.id) AS total_modules,
    COUNT(m.id) FILTER (WHERE ms.code = 'repaired') AS repaired_count,
    COUNT(m.id) FILTER (WHERE ms.code = 'not_repairable') AS not_repairable_count,
    COALESCE(SUM(mr.price), 0) AS total_value
FROM packages p
JOIN package_statuses ps ON p.current_status_id = ps.id
LEFT JOIN clients c ON c.user_id = p.client_id
LEFT JOIN modules m ON m.package_id = p.id AND m.deleted_at IS NULL
LEFT JOIN module_statuses ms ON m.current_status_id = ms.id
LEFT JOIN module_repairs mr ON mr.module_id = m.id
GROUP BY p.id, ps.code, ps.name, c.company_name;
