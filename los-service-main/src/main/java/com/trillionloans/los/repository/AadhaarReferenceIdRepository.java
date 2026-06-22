package com.trillionloans.los.repository;

import com.trillionloans.los.constant.DocumentType;
import com.trillionloans.los.model.entity.ClientKycDetailsEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AadhaarReferenceIdRepository
    extends R2dbcRepository<ClientKycDetailsEntity, Long> {

  Mono<ClientKycDetailsEntity> findFirstByClientIdAndDocumentTypeOrderByIdDesc(
      String clientId, DocumentType documentType);
}
