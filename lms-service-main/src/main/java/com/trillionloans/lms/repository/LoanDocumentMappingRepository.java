package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.LoanDocumentMappingEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanDocumentMappingRepository
    extends R2dbcRepository<LoanDocumentMappingEntity, Long> {}
