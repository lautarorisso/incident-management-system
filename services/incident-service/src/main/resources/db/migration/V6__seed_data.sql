-- V6: Datos demo idempotentes para pruebas manuales.
-- Solo incidents: teams y users viven en user_db, no en esta base.
-- ON CONFLICT (id) DO NOTHING => seguro de re-aplicar.
-- assignee_id/team_id son UUIDs sintéticos (sin FK local): ejercitan los
-- índices parciales de V2 y las queries por assignee/team.

INSERT INTO incidents (id, title, description, status, priority, assignee_id, team_id, created_at, updated_at) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Production API latency spike',          'P99 rising after deploy 4.2.1',             'OPEN',        'CRITICAL', '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000011', now() - INTERVAL '3 hours',  now() - INTERVAL '3 hours'),
    ('10000000-0000-0000-0000-000000000002', 'Auth provider certificate expiry',      'OIDC certificate expiring in 72h',          'IN_PROGRESS', 'HIGH',     '20000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000011', now() - INTERVAL '10 hours', now() - INTERVAL '2 hours'),
    ('10000000-0000-0000-0000-000000000003', 'Broken report download',                'CSV export returns 500 for date range',     'OPEN',        'MEDIUM',   NULL,                                             '20000000-0000-0000-0000-000000000012', now() - INTERVAL '26 hours', now() - INTERVAL '26 hours'),
    ('10000000-0000-0000-0000-000000000004', 'Logo missing in invoice emails',        'Invoice attachments missing brand assets',  'RESOLVED',    'LOW',      '20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000013', now() - INTERVAL '4 days',   now() - INTERVAL '2 days'),
    ('10000000-0000-0000-0000-000000000005', 'Database connection pool exhaustion',   'Connections not returned after timeouts',   'IN_PROGRESS', 'CRITICAL', '20000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000012', now() - INTERVAL '30 hours', now() - INTERVAL '1 hour'),
    ('10000000-0000-0000-0000-000000000006', 'Stale cache on team dashboard',         'Aggregates not refreshed after reindex',    'OPEN',        'LOW',      NULL,                                             NULL,                                     now() - INTERVAL '5 days',   now() - INTERVAL '5 days'),
    ('10000000-0000-0000-0000-000000000007', 'Login redirect loop on Safari',         'OAuth callback loops on Safari 18',         'CLOSED',      'MEDIUM',   '20000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000011', now() - INTERVAL '12 days',  now() - INTERVAL '10 days'),
    ('10000000-0000-0000-0000-000000000008', 'Webhook signature verification',        'HMAC check fails for duplicated deliveries', 'RESOLVED',    'HIGH',     '20000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000013', now() - INTERVAL '6 days',   now() - INTERVAL '5 days'),
    ('10000000-0000-0000-0000-000000000009', 'Nightly batch job overrun',             'Backup job exceeds maintenance window',     'OPEN',        'MEDIUM',   '20000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000012', now() - INTERVAL '8 hours',  now() - INTERVAL '8 hours'),
    ('10000000-0000-0000-0000-000000000010', 'TLS 1.0 still accepted',                'Old clients negotiate deprecated cipher',   'IN_PROGRESS', 'HIGH',     '20000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000011', now() - INTERVAL '2 days',   now() - INTERVAL '20 hours')
ON CONFLICT (id) DO NOTHING;
