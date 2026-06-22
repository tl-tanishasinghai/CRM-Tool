package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.QcCheckEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface QcCheckRepository extends R2dbcRepository<QcCheckEntity, Long> {}
