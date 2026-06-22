package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.ReKycProductConfigEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface ReKycProductConfigRepository
    extends R2dbcRepository<ReKycProductConfigEntity, Long> {}
