-- Schema for charge_audit_log
CREATE TABLE IF NOT EXISTS public.charge_audit_log (
    id bigserial NOT NULL,
    run_id int8 NOT NULL,
    external_id varchar(128) NOT NULL,
    loan_id int8 NOT NULL,
    installment_no int4 NOT NULL,
    short_code varchar(32) NOT NULL,
    charge_name varchar(256) NOT NULL,
    product_code varchar(32) NOT NULL,
    charge_date date NOT NULL,
    m2p_charge_type_id int8 NOT NULL,
    outstanding numeric(18, 6) NOT NULL,
    base numeric(18, 6) NOT NULL,
    gst numeric(18, 6) NOT NULL,
    total numeric(18, 6) NOT NULL,
    post_status varchar(16) NOT NULL,
    post_ref varchar(256) NULL,
    message text NULL,
    created_at timestamp DEFAULT now() NOT NULL,
    updated_at timestamp NULL,
    charge_posted_date date NULL,
    CONSTRAINT charge_audit_log_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_charge_audit_date ON public.charge_audit_log USING btree (charge_date);
CREATE INDEX IF NOT EXISTS ix_charge_audit_status ON public.charge_audit_log USING btree (post_status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_charge_audit_external ON public.charge_audit_log USING btree (external_id);

-- Schema for charge_run_log
CREATE TABLE IF NOT EXISTS public.charge_run_log (
    id bigserial NOT NULL,
    run_date date NOT NULL,
    started_at timestamp DEFAULT now() NOT NULL,
    completed_at timestamp NULL,
    status varchar(16) NOT NULL,
    total_emis_processed int8 DEFAULT 0 NULL,
    total_charges_attempted int8 DEFAULT 0 NULL,
    total_posted int8 DEFAULT 0 NULL,
    total_skipped int8 DEFAULT 0 NULL,
    total_failed int8 DEFAULT 0 NULL,
    error_message text NULL,
    CONSTRAINT charge_run_log_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS ix_charge_run_log_run_date ON public.charge_run_log USING btree (run_date);

-- Schema for charge_audit_log_history
create table if not exists public.charge_audit_log_history (
    id bigserial not null,
    run_id int8 not null,
    external_id varchar(128) not null,
    charge_name varchar(256) not null,
    product_code varchar(32) not null,
    charge_posted_date date not null,
    m2p_charge_type_id int8 not null,
    outstanding numeric(18, 6) null,
    base numeric(18, 6) not null,
    gst numeric(18, 6) not null,
    total numeric(18, 6) not null,
    post_status varchar(16) not null,
    post_ref varchar(256) null,
    message text null,
    created_at timestamp default now() not null,
    constraint charge_audit_log_history_pkey primary key (id)
);

CREATE INDEX IF NOT EXISTS ix_charge_audit_hist_run_id ON public.charge_audit_log_history (run_id);
CREATE INDEX IF NOT EXISTS ix_charge_audit_hist_external ON public.charge_audit_log_history (external_id);
CREATE INDEX IF NOT EXISTS ix_charge_audit_hist_status ON public.charge_audit_log_history (post_status);

-- Schema for loan_restructure_eligibility_master
CREATE TABLE IF NOT EXISTS public.loan_restructure_eligibility_master (
    id bigserial PRIMARY KEY,
    lan int8,
    lead_id int8,
    client_id int8,
    eligible boolean,
    created_at timestamp,
    last_updated_at timestamp
);

CREATE INDEX IF NOT EXISTS ix_lr_eligibility_lan
    ON public.loan_restructure_eligibility_master (lan);


-- Schema for loan_application_restructure_details
CREATE TABLE IF NOT EXISTS public.loan_application_restructure_details (
    id bigserial PRIMARY KEY,
    lan int8,
    lead int8,
    client int8,
    eligibility boolean,
    eligibility_data jsonb,
    restructure varchar(64),
    restructure_id int8,
    customer_name varchar(255),
    mobile_number varchar(32),
    created_at timestamp,
    updated_at timestamp,
    approved_on timestamp,
    signed_doc_id varchar(255),
    signed_url text
);

CREATE INDEX IF NOT EXISTS ix_lard_lan_id_desc
    ON public.loan_application_restructure_details (lan, id DESC);

CREATE INDEX IF NOT EXISTS ix_lard_lan_restructure_id
    ON public.loan_application_restructure_details (lan, restructure_id);


-- Schema for loan_restructure_notification_tracking
CREATE TABLE IF NOT EXISTS public.loan_restructure_notification_tracking (
    id bigserial PRIMARY KEY,
    restructure_details_id int8 NOT NULL,
    loan_account_number int8,
    status varchar(64),
    attempt_count int4,
    last_error text,
    s3_file_path text,
    customer_name varchar(255),
    mobile_number varchar(32),
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT fk_lrnt_restructure_details
        FOREIGN KEY (restructure_details_id)
        REFERENCES public.loan_application_restructure_details (id)
);

CREATE INDEX IF NOT EXISTS ix_lrnt_restructure_details_id_desc
    ON public.loan_restructure_notification_tracking (restructure_details_id, id DESC);

CREATE INDEX IF NOT EXISTS ix_lrnt_status
    ON public.loan_restructure_notification_tracking (status);


-- Credit Line Mark Repayment

CREATE table if not exists credit_line_mark_repayment_records (
    id BIGSERIAL PRIMARY KEY,
    line_id VARCHAR(100),
    drawdown_transaction_id VARCHAR(100),
    amount DECIMAL(15, 2),
    transaction_id VARCHAR(100),
    payment_type_id INT,
    reference_number VARCHAR(100),
    transaction_time BIGINT,
	created_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL
);

-- For retrieving all repayments linked to a specific credit line
CREATE INDEX idx_repayment_line_id ON credit_line_mark_repayment_records (line_id);

-- For lookups by the external transaction ID (essential for reconciliation)
CREATE INDEX idx_repayment_transaction_id ON credit_line_mark_repayment_records (transaction_id);

-- For linking repayments back to the specific drawdown they are paying off
CREATE INDEX idx_repayment_drawdown_id ON credit_line_mark_repayment_records (drawdown_transaction_id);

-- For chronological sorting and finding the "last transaction"
CREATE INDEX idx_repayment_transaction_time ON credit_line_mark_repayment_records (transaction_time DESC);

CREATE INDEX idx_line_time_composite ON credit_line_mark_repayment_records (line_id, transaction_time DESC);
