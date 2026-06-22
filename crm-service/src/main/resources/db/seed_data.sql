-- Demo seed data for local/dev environments. Safe to re-run (ON CONFLICT DO NOTHING).
-- Ops can skip this file in production and manage users via admin APIs.

INSERT INTO staff_users (id, name, email, role, status, lead_id, created_at)
VALUES
    ('admin-1', 'CRM Admin', 'admin@trillionloans.com', 'ADMIN', 'ACTIVE', NULL, NOW()),
    ('lead-1', 'Collections Lead', 'lead@trillionloans.com', 'LEAD', 'ACTIVE', NULL, NOW()),
    ('agent-1', 'Asha Agent', 'agent1@trillionloans.com', 'AGENT', 'ACTIVE', 'lead-1', NOW()),
    ('agent-2', 'Ravi Agent', 'agent2@trillionloans.com', 'AGENT', 'ACTIVE', 'lead-1', NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO crm_leads (
    id, lead_id, client_id, mobile_number, title, loan_account_number, loan_application_id,
    status, priority, source, assigned_agent_id, assigned_lead_id, assigned_at, created_at, updated_at
)
VALUES
    ('crm-lead-1', '1002001', 'C1002001', '9999999999', 'Repayment schedule request', 'LAN-900001', 'LA-700001',
     'ASSIGNED', 'HIGH', 'MOCK', 'agent-1', 'lead-1', NOW(), NOW(), NOW()),
    ('crm-lead-2', '1002002', 'C1002002', '8888888888', 'Settlement query for closed loan', 'LAN-900002', 'LA-700002',
     'FOLLOW_UP', 'MEDIUM', 'FRESHDESK', 'agent-1', 'lead-1', NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '15 minutes'),
    ('crm-lead-3', '1002003', 'C1002003', '7777777777', 'EMI debit failed callback', 'LAN-900003', 'LA-700003',
     'IN_PROGRESS', 'URGENT', 'INBOUND_CALL', 'agent-2', 'lead-1', NOW() - INTERVAL '15 minutes', NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '5 minutes'),
    ('crm-lead-4', '1002004', 'C1002004', '6666666666', 'Legal notice escalation', 'LAN-900004', 'LA-700004',
     'ESCALATED', 'URGENT', 'FRESHDESK', 'agent-1', 'lead-1', NOW() - INTERVAL '10 minutes', NOW() - INTERVAL '60 minutes', NOW() - INTERVAL '2 minutes'),
    ('crm-lead-5', '1002005', 'C1002005', '5555555555', 'NOC dispatched', 'LAN-900005', 'LA-700005',
     'CLOSED', 'LOW', 'MANUAL', 'agent-2', 'lead-1', NOW() - INTERVAL '2 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

INSERT INTO crm_tickets (id, lead_id, subject, status, priority, category, created_at, updated_at)
VALUES
    ('fd-1001', '1002001', 'Customer requested updated repayment schedule', 'OPEN', 'MEDIUM', 'Loan servicing',
     NOW() - INTERVAL '1 hour', NOW() - INTERVAL '20 minutes')
ON CONFLICT (id) DO NOTHING;

INSERT INTO call_logs (
    id, call_sid, parent_call_sid, lead_id, agent_id, call_direction, disposition, call_status,
    phone_number, from_number, to_number, duration_seconds, recording_url, sync_status,
    started_at, ended_at, created_at, updated_at
)
VALUES
    ('call-1001', 'exotel-seed-1001', NULL, '1002001', 'agent-1', 'INBOUND', 'CALLBACK_REQUESTED', 'completed',
     '9999999999', '9999999999', '0800000000', 185, NULL, 'MANUAL',
     NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour 56 minutes', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
    ('call-ivr-1', 'exotel-greylabs-26043', NULL, '1002001', 'agent-1', 'OUTBOUND', 'ESCALATED', 'completed',
     '9999999999', '08047111111', '9999999999', 142, NULL, 'SYNCED',
     NOW() - INTERVAL '90 minutes', NOW() - INTERVAL '88 minutes', NOW() - INTERVAL '90 minutes', NOW() - INTERVAL '90 minutes'),
    ('call-ivr-2', 'exotel-greylabs-26044', NULL, '1002002', 'agent-1', 'OUTBOUND', 'CALLBACK_REQUESTED', 'completed',
     '8888888888', '08047111111', '8888888888', 98, NULL, 'SYNCED',
     NOW() - INTERVAL '3 hours', NOW() - INTERVAL '2 hours 58 minutes', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours'),
    ('call-ivr-3', 'exotel-greylabs-resolved', NULL, '1002001', 'agent-1', 'OUTBOUND', 'RESOLVED', 'completed',
     '9999999999', '08047111111', '9999999999', 76, NULL, 'SYNCED',
     NOW() - INTERVAL '1 day', (NOW() - INTERVAL '1 day') + INTERVAL '76 seconds', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;
