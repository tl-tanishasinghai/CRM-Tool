CREATE TABLE IF NOT EXISTS callback (
                                        id SERIAL PRIMARY KEY,
                                        type VARCHAR(500),
                                        request JSONB,
                                        response JSONB,
                                        exception VARCHAR(6000),
                                        product_code VARCHAR(500),
                                        reference_id VARCHAR(500),
                                        uri VARCHAR(6000),
                                        created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS partner_master (
    id SERIAL PRIMARY KEY,
    partner_id VARCHAR(50),
    partner_name VARCHAR(50),
    product_code VARCHAR(50),
    product_name VARCHAR(50),
    product_type VARCHAR(50),
    status VARCHAR(10),
    office_name VARCHAR(200),
    product_id_m2p VARCHAR(50),
    is_remitx_enabled BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS product_config_master (
    id SERIAL PRIMARY KEY,
    product_code VARCHAR(50),
    partner_code VARCHAR(50),
    product_json JSONB
);

CREATE TABLE IF NOT EXISTS los_bre (
    id SERIAL PRIMARY KEY,
    external_id VARCHAR(20) ,
    bre_type VARCHAR(20) ,
    request JSONB  ,
    response JSONB ,
    stage VARCHAR(50) ,
    status VARCHAR(10) ,
    callback_id INTEGER ,
    product_code VARCHAR(10) ,
    is_active BOOLEAN,
    scienaptic_status VARCHAR(50),
    retry_count BIGINT,
    rejected_count BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS scienaptic (
    id SERIAL PRIMARY KEY,
    external_id VARCHAR(20) NOT NULL,
    bre_type VARCHAR(20) ,
    request JSONB  ,
    response JSONB ,
    is_active BOOLEAN,
    created_at TIMESTAMP,
    scienaptic_status VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS lead_acknowledgement (
    id SERIAL PRIMARY KEY,
    loan_application_id VARCHAR(50) NOT NULL,
    acknowledgement_status VARCHAR(20) NOT NULL,
    error_message TEXT,
    acknowledgement_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS loan_form (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(320),
    phone VARCHAR(100),
    loan_type VARCHAR(255),
    consent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS partnership_form (
    id SERIAL PRIMARY KEY,  -- Auto-incremented primary key
    partnership_type VARCHAR(255) CHECK (partnership_type IN ('Co-lending', 'Channel')),  -- Enum-like field
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(320),
    phone VARCHAR(100),
    organization_name VARCHAR(255),
    designation_name VARCHAR(255),
    consent BOOLEAN NOT NULL DEFAULT FALSE,  -- Tinyint(1) replaced by BOOLEAN
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0  -- Tinyint(4) replaced by SMALLINT
);

CREATE TABLE IF NOT EXISTS rules (
                                     id SERIAL PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    product_code VARCHAR(255) NOT NULL,
    description TEXT,
    priority INTEGER NOT NULL,
    condition TEXT NOT NULL,
    action TEXT NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


-- ====== DEPRECATED: disburse_transactions table ======
-- this table is no longer used. all disbursal tracking moved to disbursal_registry.
-- keeping for backward compatibility during migration. drop after migration is complete.
-- CREATE TABLE disburse_transactions(
--                                       id serial NOT NULL,
--                                       external_id varchar(20) NOT NULL,
--                                       product_code varchar(10) NOT NULL,
--                                       disburse_type varchar(50) NOT NULL,
--                                       disburse_status varchar(50) NOT NULL,
--                                       transaction_status varchar(50) NOT NULL,
--                                       created_at timestamp NOT NULL,
--                                       updated_at timestamp NOT NULL
-- );

CREATE TABLE IF NOT EXISTS mandate_registration_details (
    id BIGSERIAL PRIMARY KEY,                                -- Auto-incrementing primary key
    client_id VARCHAR(255) NOT NULL,                         -- Indexed (see below)
    loan_id VARCHAR(255) NOT NULL,                           -- Indexed (see below)
    mandate_id VARCHAR(255) NOT NULL,                        -- Indexed (see below)
    auth_mode VARCHAR(100),
    amount VARCHAR(50),
    frequency_type VARCHAR(100),
    vendor_name VARCHAR(255),
    is_recurring BOOLEAN,
    generate_access_token BOOLEAN,
    state VARCHAR(30),                                       -- Indexed (see below)
    first_collection_date TIMESTAMP,                         -- LocalDateTime mapping
    final_collection_date TIMESTAMP,
    required_otp BOOLEAN,
    notify_customer BOOLEAN,
    partner_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),                      -- Indexed, with default
    updated_at TIMESTAMP,
    updated_at_los TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 1,                      -- Default value
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE                -- Default value
    );


-- Indexes for efficient querying
CREATE INDEX idx_mandate_registration_client_id   ON mandate_registration_details (client_id);
CREATE INDEX idx_mandate_registration_loan_id     ON mandate_registration_details (loan_id);
CREATE INDEX idx_mandate_registration_mandate_id  ON mandate_registration_details (mandate_id);
CREATE INDEX idx_mandate_registration_state       ON mandate_registration_details (state);
CREATE INDEX idx_mandate_registration_created_at  ON mandate_registration_details (created_at);
CREATE INDEX idx_mandate_registration_partner_id  ON mandate_registration_details (partner_id);

CREATE TABLE IF NOT EXISTS customer_data_variance (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    client_id VARCHAR(255) NOT NULL,
    changed_fields JSONB NOT NULL,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index
CREATE INDEX IF NOT EXISTS idx_customer_data_variance_client_id ON customer_data_variance(client_id);
CREATE INDEX IF NOT EXISTS idx_customer_data_variance_detected_at ON customer_data_variance(detected_at);



CREATE TABLE IF NOT EXISTS disbursal_registry (
    id bigserial NOT NULL,
    reference_id1 varchar(100) NOT NULL,
    client_id varchar(100) NULL,
    client_name varchar(250) NULL,
    reference_id2 varchar(100) NULL,
    product_code varchar(100) NOT NULL,
    disburse_type varchar(100) NOT NULL,
    disburse_status varchar(100) NOT NULL,
    transaction_date varchar(100) NULL,
    batch_id uuid NULL,
    ifsc_code varchar(100) NULL,
    bank_holder_name varchar(255) NULL,
    bank_name varchar(255) NULL,
    bank_name_number varchar(255) NULL,
    gross_disbursal_amount numeric(15, 2) NULL,
    net_disbursal_amount numeric(15, 2) NULL,
    balance_transfer_outstanding numeric(15, 2) NULL,
    partner varchar(100) NULL,
    balance_transfer_customer_existing_loan_id varchar(100) NULL,
    utr_number varchar(100) NULL,
    bank_code varchar(100) NULL,
    bank_account_id varchar(100) NULL,
    bank_account_number varchar(100) NULL,
    anchor_id varchar(200) NULL,
    failure_reason text NULL,
    secondary_failure_reason text NULL,
    is_hydrated bool DEFAULT false NULL,
    hydrated_started_at timestamp NULL,
    hydrated_completed_at timestamp NULL,
    created_at timestamp DEFAULT now() NULL,
    updated_at timestamp DEFAULT now() NULL,
    "version" int4 DEFAULT 1 NULL,
    is_deleted bool DEFAULT false NULL,
    CONSTRAINT disbursal_registry_pkey PRIMARY KEY (id),
    CONSTRAINT uk_disbursal_registry_reference_id1 UNIQUE (reference_id1)
    );

-- indexes for disbursal_registry
CREATE INDEX IF NOT EXISTS idx_disbursal_registry_reference_id1 ON disbursal_registry(reference_id1);
CREATE INDEX IF NOT EXISTS idx_disbursal_registry_reference_id2 ON disbursal_registry(reference_id2);
CREATE INDEX IF NOT EXISTS idx_disbursal_registry_client_id ON disbursal_registry(client_id);
CREATE INDEX IF NOT EXISTS idx_disbursal_registry_batch_id ON disbursal_registry(batch_id);
CREATE INDEX IF NOT EXISTS idx_disbursal_registry_disburse_status ON disbursal_registry(disburse_status);


-- ====== DISBURSAL BATCH TABLE ======

CREATE TABLE disbursal_batch (
    id uuid PRIMARY KEY,
    batch_status character varying(50) NOT NULL,
    net_amount double precision,
    total_records integer,
    hydrated_records integer,
    file_checksum character varying(255),
    error_details text,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone DEFAULT now(),
    version integer DEFAULT 1,
    is_deleted boolean DEFAULT false,
    created_by character varying(100)
);

CREATE UNIQUE INDEX disbursal_batch_pkey ON disbursal_batch(id uuid_ops);

-- ====== reverse feed batch table ======
-- tracks each reverse feed file upload
CREATE TABLE IF NOT EXISTS reverse_feed_batch (
    id BIGSERIAL PRIMARY KEY,
    batch_id UUID NOT NULL UNIQUE,
    file_name VARCHAR(255),
    total_records INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    pending_count INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL,              -- processing, completed, partial_failure
    uploaded_by VARCHAR(100),
    uploaded_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reverse_feed_batch_batch_id ON reverse_feed_batch(batch_id);
CREATE INDEX IF NOT EXISTS idx_reverse_feed_batch_status ON reverse_feed_batch(status);

-- ====== reverse feed batch item table ======
-- individual items from reverse feed (same loan can appear in multiple batches)
-- unified naming: reference_id1 = loan_application_id/transaction_id, reference_id2 = loan_account_number/line_id
-- excel "loan account number" stored in ref1 (line products) or ref2 (non-line products) based on product type
CREATE TABLE IF NOT EXISTS reverse_feed_batch_item (
    id BIGSERIAL PRIMARY KEY,
    batch_id UUID NOT NULL,
    reference_id1 VARCHAR(50),                    -- loan_application_id (pl/cl) or transaction_id
    reference_id2 VARCHAR(100),                   -- loan_account_number (pl/cl) or line_id
    transaction_status VARCHAR(50),               -- e (executed/success), r (rejected/failure)
    utr_number VARCHAR(100),
    transaction_rejection_reason VARCHAR(500),
    amount DECIMAL(15, 2),
    transaction_date TIMESTAMP,
    sync_status VARCHAR(50) NOT NULL,             -- pending, success, failed
    m2p_response TEXT,
    error_message TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_reverse_feed_batch_item_batch
        FOREIGN KEY (batch_id) REFERENCES reverse_feed_batch(batch_id)
);

CREATE INDEX IF NOT EXISTS idx_reverse_feed_batch_item_batch_id ON reverse_feed_batch_item(batch_id);
CREATE INDEX IF NOT EXISTS idx_reverse_feed_batch_item_reference_id1 ON reverse_feed_batch_item(reference_id1);
CREATE INDEX IF NOT EXISTS idx_reverse_feed_batch_item_reference_id2 ON reverse_feed_batch_item(reference_id2);
CREATE INDEX IF NOT EXISTS idx_reverse_feed_batch_item_sync_status ON reverse_feed_batch_item(sync_status);



-- Invoice Drawdown Module
CREATE TABLE invoices (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    partner_id TEXT NOT NULL,
    anchor_id TEXT NOT NULL,
    invoice_number TEXT NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    invoice_date DATE NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    hash_key TEXT NOT NULL
);


CREATE UNIQUE INDEX uniq_invoices_hash_key
ON invoices (hash_key);

CREATE TABLE drawdowns (
    id BIGSERIAL PRIMARY KEY,
    partner_id VARCHAR(255) NOT NULL,
    anchor_id VARCHAR(255) NOT NULL,
    amount NUMERIC(20, 2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NULL,
    line_id VARCHAR(255) NOT NULL,
    metadata JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE drawdown_invoice_mappings (
    id BIGSERIAL PRIMARY KEY,
    drawdown_id BIGINT NOT NULL REFERENCES drawdowns(id) ON DELETE CASCADE,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );

-- Index for partner reporting
CREATE INDEX idx_drawdowns_partner_anchor ON drawdowns (partner_id, anchor_id);
-- GIN Index for searching inside the JSONB metadata (e.g., searching for a specific charge code)
CREATE INDEX idx_drawdowns_metadata_jsonb ON drawdowns USING GIN (metadata);

-- Credit Line table for storing credit line details
CREATE TABLE IF NOT EXISTS credit_line (
    id BIGSERIAL PRIMARY KEY,
    lead_id VARCHAR(50) NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    credit_limit NUMERIC(20, 2),
    tenure_type VARCHAR(20) CHECK (tenure_type IN ('DAYS', 'MONTHS')),
    tenure_value INTEGER,
    m2p_credit_line_id VARCHAR(255),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CREATED', 'APPROVED', 'ACTIVE')),
    limit_created_at TIMESTAMP,
    limit_approved_at TIMESTAMP,
    limit_activated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for credit_line table
CREATE INDEX IF NOT EXISTS idx_credit_line_lead_id ON credit_line (lead_id);
CREATE INDEX IF NOT EXISTS idx_credit_line_m2p_credit_line_id ON credit_line (m2p_credit_line_id);



-- Invoice Drawdown Module
CREATE TABLE invoices (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    partner_id TEXT NOT NULL,
    anchor_id TEXT ,
    invoice_number TEXT NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    invoice_date DATE NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    hash_key TEXT NOT NULL
);


CREATE UNIQUE INDEX uniq_invoices_hash_key
ON invoices (hash_key);

CREATE TABLE drawdowns (
    id BIGSERIAL PRIMARY KEY,
    partner_id VARCHAR(255) NOT NULL,
    anchor_id VARCHAR(255) ,
    amount NUMERIC(20, 2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NULL,
    line_id VARCHAR(255) NOT NULL,
    external_id VARCHAR(255) NULL,
    metadata JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Unique per partner: same externalId from same partner = one drawdown (across all lines)
CREATE UNIQUE INDEX uniq_drawdowns_external_id_partner
ON drawdowns (external_id, partner_id) WHERE external_id IS NOT NULL;

CREATE TABLE drawdown_invoice_mappings (
    id BIGSERIAL PRIMARY KEY,
    drawdown_id BIGINT NOT NULL REFERENCES drawdowns(id) ON DELETE CASCADE,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );

-- Index for partner reporting
CREATE INDEX idx_drawdowns_partner_anchor ON drawdowns (partner_id, anchor_id);
-- GIN Index for searching inside the JSONB metadata (e.g., searching for a specific charge code)
CREATE INDEX idx_drawdowns_metadata_jsonb ON drawdowns USING GIN (metadata);

-- Credit Line table for storing credit line details
CREATE TABLE IF NOT EXISTS credit_line (
    id BIGSERIAL PRIMARY KEY,
    lead_id VARCHAR(50) NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    credit_limit NUMERIC(20, 2),
    tenure_type VARCHAR(20) CHECK (tenure_type IN ('DAYS', 'MONTHS')),
    tenure_value INTEGER,
    m2p_credit_line_id VARCHAR(255),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CREATED', 'APPROVED', 'ACTIVE')),
    limit_created_at TIMESTAMP,
    limit_approved_at TIMESTAMP,
    limit_activated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for credit_line table
CREATE INDEX IF NOT EXISTS idx_credit_line_lead_id ON credit_line (lead_id);
CREATE INDEX IF NOT EXISTS idx_credit_line_m2p_credit_line_id ON credit_line (m2p_credit_line_id);


CREATE TABLE IF NOT EXISTS kyc_qc (
    id BIGSERIAL PRIMARY KEY,
    loan_application_id VARCHAR(255),
    client_id VARCHAR(255),
    xml_ts VARCHAR(500),
    final_name_match_score VARCHAR(255),
    final_face_match_score VARCHAR(255),
    karza_name_match_score VARCHAR(255),
    karza_face_match_score VARCHAR(255),
    analytic_name_match_score VARCHAR(255),
    analytic_face_match_score VARCHAR(255),
    final_name_match_status VARCHAR(255),
    final_face_match_status VARCHAR(255),
    xml_validity_status VARCHAR(255),
    kyc_type VARCHAR(100),
    product_code VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version INT DEFAULT 1,
    is_deleted BOOLEAN DEFAULT FALSE
    );

-- Drawdown document references (reference-only, no payload); details in metadata JSONB
CREATE TABLE IF NOT EXISTS drawdown_documents (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    partner_id VARCHAR(100),
    line_id VARCHAR(255),
    tag VARCHAR(100),
    file_path TEXT,
    s3_path TEXT,
    m2p_document_id INTEGER,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_drawdown_documents_entity_line
ON drawdown_documents (entity_type, entity_id, line_id);

CREATE INDEX IF NOT EXISTS idx_drawdown_documents_document_type
ON drawdown_documents (document_type);

CREATE INDEX IF NOT EXISTS idx_drawdown_documents_line_id
ON drawdown_documents (line_id);

CREATE INDEX IF NOT EXISTS idx_drawdown_documents_partner_line
ON drawdown_documents (partner_id, line_id)
WHERE partner_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_drawdown_documents_created_at
ON drawdown_documents (created_at DESC);


-- Indexes
CREATE INDEX IF NOT EXISTS idx_kyc_qc_loan_application_id ON kyc_qc (loan_application_id);
CREATE INDEX IF NOT EXISTS idx_kyc_qc_client_id ON kyc_qc (client_id);
CREATE INDEX IF NOT EXISTS idx_kyc_qc_final_name_match_status ON kyc_qc (final_name_match_status);
CREATE INDEX IF NOT EXISTS idx_kyc_qc_final_face_match_status ON kyc_qc (final_face_match_status);
CREATE INDEX IF NOT EXISTS idx_kyc_qc_xml_validity_status ON kyc_qc (xml_validity_status);
CREATE INDEX IF NOT EXISTS idx_kyc_qc_product_code ON kyc_qc (product_code);
CREATE INDEX IF NOT EXISTS idx_kyc_qc_created_at ON kyc_qc (created_at);
CREATE INDEX IF NOT EXISTS idx_kyc_qc_updated_at ON kyc_qc (updated_at);

-- Migration: Add columns to loan_application_restructure_details for signed doc tracking
-- Run manually if table exists: ALTER TABLE loan_application_restructure_details
--     ADD COLUMN customer_name character varying(255),
--     ADD COLUMN mobile_number character varying(20),
--     ADD COLUMN signed_doc_id character varying(255),
--     ADD COLUMN signed_url text;
