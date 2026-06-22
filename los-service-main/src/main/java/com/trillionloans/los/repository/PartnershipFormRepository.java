package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.PartnershipFormEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnershipFormRepository extends R2dbcRepository<PartnershipFormEntity, Long> {}
