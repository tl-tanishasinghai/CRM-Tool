package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.OfferExpiryRejectionEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferExpiryRejectionRepository
    extends R2dbcRepository<OfferExpiryRejectionEntity, String> {}
