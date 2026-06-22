-- Drawdown document references (no file payload). Safe to run on empty DB or idempotent refresh.
-- Includes file_path, S3 key, M2P id, and metadata in one table.

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

-- List/fetch by logical parent (invoice or drawdown) within a line
CREATE INDEX IF NOT EXISTS idx_drawdown_documents_entity_line
    ON drawdown_documents (entity_type, entity_id, line_id);

-- Filter by document category
CREATE INDEX IF NOT EXISTS idx_drawdown_documents_document_type
    ON drawdown_documents (document_type);

-- Line-level scans (e.g. all docs on a credit line)
CREATE INDEX IF NOT EXISTS idx_drawdown_documents_line_id
    ON drawdown_documents (line_id);

-- Partner-scoped invoice docs
CREATE INDEX IF NOT EXISTS idx_drawdown_documents_partner_line
    ON drawdown_documents (partner_id, line_id)
    WHERE partner_id IS NOT NULL;

-- Stable ordering for GET APIs
CREATE INDEX IF NOT EXISTS idx_drawdown_documents_created_at
    ON drawdown_documents (created_at DESC);

-- Superseded by idx_drawdown_documents_entity_line (same leading columns)
DROP INDEX IF EXISTS idx_drawdown_documents_entity;
