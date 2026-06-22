package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.MasterDocument;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MasterTagConfigRepository extends ReactiveCrudRepository<MasterDocument, Long> {
  // Custom query to fetch all active PSL tags
  Flux<MasterDocument> findAllByIsPslTrueAndIsActiveTrue();
}
