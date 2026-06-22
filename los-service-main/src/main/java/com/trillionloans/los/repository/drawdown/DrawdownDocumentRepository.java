package com.trillionloans.los.repository.drawdown;

import com.trillionloans.los.model.entity.DrawdownDocument;
import java.util.Collection;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DrawdownDocumentRepository extends ReactiveCrudRepository<DrawdownDocument, Long> {

  Flux<DrawdownDocument> findAllByEntityTypeAndEntityIdAndLineIdOrderByCreatedAtDesc(
      String entityType, Long entityId, String lineId);

  Flux<DrawdownDocument> findAllByEntityTypeAndEntityIdInAndLineIdOrderByCreatedAtDesc(
      String entityType, Collection<Long> entityIds, String lineId);
}
