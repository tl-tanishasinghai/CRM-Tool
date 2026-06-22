package com.trillionloans.los.repository.drawdown;

import com.trillionloans.los.model.entity.DrawdownAdditionalDetails;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface DrawdownAdditionalDetailsRepository
    extends ReactiveCrudRepository<DrawdownAdditionalDetails, Long> {

  /**
   * Finds additional details by drawdown ID.
   *
   * @param drawdownId the drawdown ID
   * @return the additional details if found
   */
  Mono<DrawdownAdditionalDetails> findByDrawdownId(Long drawdownId);

  /**
   * Finds additional details by loan account number (LAN).
   *
   * @param loanAccountNumber the loan account number
   * @return the additional details if found
   */
  Mono<DrawdownAdditionalDetails> findByLoanAccountNumber(Long loanAccountNumber);
}
