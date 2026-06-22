package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.MClientUpdateAuditLog;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface MClientUpdateAuditRepository
    extends R2dbcRepository<MClientUpdateAuditLog, Long> {}
