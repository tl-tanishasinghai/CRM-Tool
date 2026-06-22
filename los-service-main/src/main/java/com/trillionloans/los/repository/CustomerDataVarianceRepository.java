package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.CustomerDataVariance;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface CustomerDataVarianceRepository
    extends R2dbcRepository<CustomerDataVariance, Long> {}
