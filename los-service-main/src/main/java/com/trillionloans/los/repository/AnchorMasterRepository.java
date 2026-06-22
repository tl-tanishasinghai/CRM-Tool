package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.AnchorMaster;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AnchorMasterRepository extends ReactiveCrudRepository<AnchorMaster, Long> {

  Mono<AnchorMaster> findByAnchorId(String anchorId);

  Mono<AnchorMaster> findByPan(String pan);
}
