-- Migration: Scope external_id uniqueness by partner_id only
-- Run this if you have the old index (external_id only or external_id+partner_id+line_id)
-- Constraint: one externalId per partner across all lines

DROP INDEX IF EXISTS uniq_drawdowns_external_id;
DROP INDEX IF EXISTS uniq_drawdowns_external_id_partner_line;

CREATE UNIQUE INDEX IF NOT EXISTS uniq_drawdowns_external_id_partner
ON drawdowns (external_id, partner_id) WHERE external_id IS NOT NULL;
