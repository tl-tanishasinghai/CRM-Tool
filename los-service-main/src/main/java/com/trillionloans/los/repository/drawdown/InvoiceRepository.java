package com.trillionloans.los.repository.drawdown;

import com.trillionloans.los.model.entity.Invoice;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface InvoiceRepository extends ReactiveCrudRepository<Invoice, Long> {

  Mono<Invoice> findByHashKey(String hashKey);

  Mono<Boolean> existsByHashKey(String hashKey);
}
