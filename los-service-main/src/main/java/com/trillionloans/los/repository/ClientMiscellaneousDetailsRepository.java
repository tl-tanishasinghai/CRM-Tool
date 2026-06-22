package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.ClientMiscellaneousDetails;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientMiscellaneousDetailsRepository
    extends R2dbcRepository<ClientMiscellaneousDetails, Long> {}
