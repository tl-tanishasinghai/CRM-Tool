-- CRM service bootstrap schema (apply manually in prod, or via local profile init)
-- Matches Trillion Loans conventions: snake_case tables/columns, idx_ indexes

CREATE TABLE IF NOT EXISTS staff_users (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    lead_id         VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_users_email ON staff_users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_staff_users_role_status ON staff_users (role, status);
CREATE INDEX IF NOT EXISTS idx_staff_users_lead_id ON staff_users (lead_id);

CREATE TABLE IF NOT EXISTS auth_sessions (
    token           VARCHAR(128) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL REFERENCES staff_users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_auth_sessions_user_id ON auth_sessions (user_id);

CREATE TABLE IF NOT EXISTS crm_leads (
    id                      VARCHAR(64) PRIMARY KEY,
    lead_id                 VARCHAR(64) NOT NULL,
    client_id               VARCHAR(64),
    mobile_number           VARCHAR(32),
    title                   VARCHAR(512) NOT NULL,
    loan_account_number     VARCHAR(64),
    loan_application_id     VARCHAR(64),
    status                  VARCHAR(32) NOT NULL,
    priority                VARCHAR(32) NOT NULL,
    source                  VARCHAR(32) NOT NULL,
    assigned_agent_id       VARCHAR(64),
    assigned_lead_id        VARCHAR(64),
    assigned_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_leads_lead_id ON crm_leads (lead_id);
CREATE INDEX IF NOT EXISTS idx_crm_leads_assigned_agent_id ON crm_leads (assigned_agent_id);
CREATE INDEX IF NOT EXISTS idx_crm_leads_assigned_lead_id ON crm_leads (assigned_lead_id);
CREATE INDEX IF NOT EXISTS idx_crm_leads_updated_at ON crm_leads (updated_at DESC);

CREATE TABLE IF NOT EXISTS crm_tickets (
    id              VARCHAR(64) PRIMARY KEY,
    lead_id         VARCHAR(64) NOT NULL,
    subject         VARCHAR(512) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    priority        VARCHAR(32) NOT NULL,
    category        VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_crm_tickets_lead_id ON crm_tickets (lead_id);

CREATE TABLE IF NOT EXISTS call_logs (
    id                  VARCHAR(64) PRIMARY KEY,
    call_sid            VARCHAR(128),
    parent_call_sid     VARCHAR(128),
    lead_id             VARCHAR(64),
    agent_id            VARCHAR(64),
    call_direction      VARCHAR(32),
    disposition         VARCHAR(64),
    call_status         VARCHAR(64),
    phone_number        VARCHAR(32),
    from_number         VARCHAR(32),
    to_number           VARCHAR(32),
    duration_seconds    INTEGER,
    recording_url       VARCHAR(512),
    source_channel      VARCHAR(64),
    freshdesk_ticket_id VARCHAR(64),
    sync_status         VARCHAR(32),
    started_at          TIMESTAMPTZ,
    ended_at            TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_call_logs_call_sid ON call_logs (call_sid) WHERE call_sid IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_call_logs_agent_id ON call_logs (agent_id);
CREATE INDEX IF NOT EXISTS idx_call_logs_lead_id ON call_logs (lead_id);
CREATE INDEX IF NOT EXISTS idx_call_logs_started_at ON call_logs (started_at DESC);

-- Migration for existing deployments (idempotent)
ALTER TABLE call_logs ADD COLUMN IF NOT EXISTS source_channel VARCHAR(64);
ALTER TABLE call_logs ADD COLUMN IF NOT EXISTS freshdesk_ticket_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_call_logs_freshdesk_ticket_id ON call_logs (freshdesk_ticket_id);
CREATE INDEX IF NOT EXISTS idx_call_logs_source_channel ON call_logs (source_channel);

CREATE TABLE IF NOT EXISTS agent_notes (
    id              VARCHAR(64) PRIMARY KEY,
    lead_id         VARCHAR(64) NOT NULL,
    agent_id        VARCHAR(64) NOT NULL,
    disposition     VARCHAR(128),
    note            TEXT NOT NULL,
    follow_up_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_notes_lead_id ON agent_notes (lead_id);

CREATE TABLE IF NOT EXISTS pii_reveal_audits (
    id              VARCHAR(64) PRIMARY KEY,
    lead_id         VARCHAR(64) NOT NULL,
    agent_id        VARCHAR(64) NOT NULL,
    field_name      VARCHAR(64) NOT NULL,
    reason          TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pii_reveal_audits_lead_id ON pii_reveal_audits (lead_id);

CREATE TABLE IF NOT EXISTS crm_assignment_state (
    state_key       VARCHAR(64) PRIMARY KEY,
    cursor_value    INTEGER NOT NULL DEFAULT 0
);

INSERT INTO crm_assignment_state (state_key, cursor_value)
VALUES ('round_robin', 0)
ON CONFLICT (state_key) DO NOTHING;
