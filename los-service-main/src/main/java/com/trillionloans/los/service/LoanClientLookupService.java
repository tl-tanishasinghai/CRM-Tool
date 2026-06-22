package com.trillionloans.los.service;

import static com.trillionloans.los.service.LoanApplicationCacheService.LAP_CLIENT_PARTNER_REDIS_KEY_PREFIX;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.model.entity.LoanClientPartnerMapEntity;
import com.trillionloans.los.repository.LoanClientPartnerMapRepository;
import com.trillionloans.los.service.db.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanClientLookupService {

  private final RedisCacheService redisCacheService;
  private final LoanClientPartnerMapRepository loanClientPartnerMapRepository;
  private final M2PWrapperApi m2PWrapperApi;

  public Mono<Integer> getClientIdForLoan(String loanApplicationId, String reason) {

    String cacheKey = LAP_CLIENT_PARTNER_REDIS_KEY_PREFIX + ":" + loanApplicationId;

    return redisCacheService
        .getObjectSilently(cacheKey, LoanClientPartnerMapEntity.class)
        .doOnNext(
            entity ->
                log.info(
                    "[GetClientIdForLoan] Source=REDIS | loanId={} | clientId={}",
                    loanApplicationId,
                    entity.getClientId()))
        .map(LoanClientPartnerMapEntity::getClientId)
        .switchIfEmpty(
            loanClientPartnerMapRepository
                .findByLoanApplicationId(Integer.valueOf(loanApplicationId))
                .doOnNext(
                    entity ->
                        log.info(
                            "[GetClientIdForLoan] Source=DATABASE | loanId={} | clientId={}",
                            loanApplicationId,
                            entity.getClientId()))
                .map(LoanClientPartnerMapEntity::getClientId)
                .switchIfEmpty(
                    m2PWrapperApi
                        .getLoanApplicationByLoanIdV2(loanApplicationId, reason)
                        .doOnNext(
                            m2pResponse ->
                                log.info(
                                    "[GetClientIdForLoan] Source=M2P_API | loanId={} | clientId={}",
                                    loanApplicationId,
                                    m2pResponse.getClientId()))
                        .flatMap(
                            m2pResponse -> {
                              Integer clientId = m2pResponse.getClientId();
                              if (clientId == null) {
                                log.error(
                                    "[GetClientIdForLoan] M2P_API returned null clientId for"
                                        + " loanId={}",
                                    loanApplicationId);
                                return Mono.error(
                                    new NotFoundException(
                                        "Client ID not found for loanApplicationId: "
                                            + loanApplicationId));
                              }
                              return Mono.just(clientId);
                            })))
        .doOnError(
            err ->
                log.error(
                    "[GetClientIdForLoan] ERROR | loanId={} | reason={} | message={}",
                    loanApplicationId,
                    reason,
                    err.getMessage(),
                    err));
  }

  public Mono<LoanClientPartnerMapEntity> getPartnerMappingForLoanApplicationId(
      String loanApplicationId) {

    return loanClientPartnerMapRepository
        .findByLoanApplicationId(Integer.valueOf(loanApplicationId))
        .doOnNext(
            entity ->
                log.info(
                    "[LOAN_APP_CLIENT_PARTNER_MAP] Source=DATABASE | loanId={} | partnerId={}",
                    loanApplicationId,
                    entity.getPartnerId()))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[LOAN_APP_CLIENT_PARTNER_MAP] Source=DATABASE | loanId={} |"
                          + " partnerId=NOT_FOUND",
                      loanApplicationId);
                  return Mono.empty();
                }));
  }

  public Mono<LoanClientPartnerMapEntity> getPartnerMappingForLanId(String lanId) {

    return loanClientPartnerMapRepository
        .findByLanId(Integer.valueOf(lanId))
        .doOnNext(
            entity ->
                log.info(
                    "[LAN_CLIENT_PARTNER_MAP] Source=DATABASE | loanId={} | partnerId={}",
                    lanId,
                    entity.getPartnerId()))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[LAN_CLIENT_PARTNER_MAP] Source=DATABASE | loanId={} | partnerId=NOT_FOUND",
                      lanId);
                  return Mono.empty();
                }));
  }
}
