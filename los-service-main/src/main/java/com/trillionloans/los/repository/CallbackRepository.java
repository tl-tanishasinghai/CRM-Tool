package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.CallbackLogEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallbackRepository
    extends R2dbcRepository<CallbackLogEntity, Long>, CallbackRepositoryCustomQueryExecutor {}
