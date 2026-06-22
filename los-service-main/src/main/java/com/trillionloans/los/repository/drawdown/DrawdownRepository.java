package com.trillionloans.los.repository.drawdown;

import com.trillionloans.los.model.entity.Drawdown;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DrawdownRepository extends ReactiveCrudRepository<Drawdown, Long> {

  /**
   * Finds a drawdown by its M2P transaction ID.
   *
   * @param transactionId the M2P transaction ID
   * @return the drawdown if found
   */
  Mono<Drawdown> findByTransactionId(String transactionId);

  Mono<Drawdown> findById(String drawdownId);

  Flux<Drawdown> findByLineId(String lineId);

  /**
   * Finds a drawdown by external ID and partner ID. Uniqueness is (external_id, partner_id) - one
   * externalId per partner across all lines.
   *
   * @param externalId the client-provided external ID (idempotency key)
   * @param partnerId the partner ID
   * @return the drawdown if found
   */
  Mono<Drawdown> findByExternalIdAndPartnerId(String externalId, String partnerId);
}
