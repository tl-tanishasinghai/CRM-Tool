package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.LoanFormEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanFormRepository extends R2dbcRepository<LoanFormEntity, Long> {}
