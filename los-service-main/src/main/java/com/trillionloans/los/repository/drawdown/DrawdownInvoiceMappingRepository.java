package com.trillionloans.los.repository.drawdown;

import com.trillionloans.los.model.entity.DrawdownInvoiceMapping;
import java.util.Collection;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DrawdownInvoiceMappingRepository
    extends ReactiveCrudRepository<DrawdownInvoiceMapping, Long> {

  /**
   * EDGE CASE CHECK: Finds any existing mappings for a collection of invoice IDs. Use this to
   * identify exactly which invoices are "dirty" before saving.
   */
  //  Flux<DrawdownInvoiceMapping> findAllByInvoiceIdIn(Collection<Long> invoiceIds);

  /**
   * Finds all invoice mappings associated with a specific drawdown. Useful for retrieving the list
   * of invoices for a "Drawdown Details" view.
   */
  Flux<DrawdownInvoiceMapping> findAllByDrawdownId(Long drawdownId);

  Flux<DrawdownInvoiceMapping> findAllByDrawdownIdIn(Collection<Long> drawdownIds);

  /** Checks if a specific invoice has any associated drawdown. */
  Mono<Boolean> existsByInvoiceId(Long invoiceId);

  @Query(
      "SELECT distinct(drawdown_id) FROM drawdown_invoice_mappings WHERE invoice_id IN"
          + " (:invoiceIds)")
  Flux<DrawdownInvoiceMapping> findAllByInvoiceIdIn(Collection<Long> invoiceIds);

  @Query("SELECT drawdown_id FROM drawdown_invoice_mappings WHERE invoice_id = :invoiceId")
  Flux<Long> findDrawdownIdsByInvoiceId(Long invoiceId);

  @Query(
      "SELECT d.status FROM drawdowns d "
          + "JOIN drawdown_invoice_mappings dim ON d.id = dim.drawdown_id "
          + "WHERE dim.invoice_id = :invoiceId")
  Flux<String> findDrawdownStatusesByInvoiceId(Long invoiceId);
}
