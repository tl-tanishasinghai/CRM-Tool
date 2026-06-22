package com.trillionloans.los.service;

import com.trillionloans.los.model.entity.CreditLinePartnerEntity;
import com.trillionloans.los.model.entity.LoanAccountPartnerEntity;
import com.trillionloans.los.model.entity.LoanApplicationClientPartnerEntity;
import com.trillionloans.los.model.entity.LoanClientPartnerMapEntity;
import com.trillionloans.los.service.db.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanApplicationCacheService {
  private final RedisCacheService redisCacheService;

  @Value("${cache.loan-app-client-partner-map.ttl-seconds:86400}")
  private long loanApplicationClientPartnerTTLSeconds;

  public static final String LAP_CLIENT_PARTNER_REDIS_KEY_PREFIX =
      "LOAN-APPLICATION-CLIENT-PARTNER";

  public static final String LAN_PARTNER_REDIS_KEY_PREFIX = "LOAN-ACCOUNT-PARTNER";

  public static final String CREDIT_LINE_PARTNER_REDIS_KEY_PREFIX = "CREDIT-LINE-PARTNER";

  /** Cache CACHE_LOAN_APP_CLIENT_PARTNER_DATA details silently (write-through cache) */
  public Mono<Void> cacheLoanApplicationClientPartner(LoanApplicationClientPartnerEntity entity) {
    log.info(
        "[CACHE_LOAN_APP_CLIENT_PARTNER_DATA] Attempting to cache loan-app-client-partner details."
            + " loanAppId={}, clientId={}, partnerId={}",
        entity.getLoanApplicationId(),
        entity.getClientId(),
        entity.getPartnerId());

    String cacheKey = buildLoanApplicationClientPartnerCacheKey(entity.getLoanApplicationId());

    return redisCacheService
        .cacheObjectSilently(cacheKey, entity, loanApplicationClientPartnerTTLSeconds)
        .doOnSuccess(
            unused ->
                log.info(
                    "[CACHE_LOAN_APP_CLIENT_PARTNER_DATA] Successfully cached"
                        + " loan-app-client-partner details. loanAppId={}, clientId={},"
                        + " partnerId={}",
                    entity.getLoanApplicationId(),
                    entity.getClientId(),
                    entity.getPartnerId()))
        .doOnError(
            e ->
                log.error(
                    "[CACHE_LOAN_APP_CLIENT_PARTNER_DATA] Failed to cache loan-app-client-partner"
                        + " details. loanAppId={}, clientId={}, partnerId={}",
                    entity.getLoanApplicationId(),
                    entity.getClientId(),
                    entity.getPartnerId()));
  }

  /** Retrieve cached loan-application-client-partner details from Redis. */
  public Mono<LoanClientPartnerMapEntity> getLoanApplicationClientPartner(
      String partnerId, String loanApplicationId) {

    String cacheKey = buildLoanApplicationClientPartnerCacheKey(loanApplicationId);

    log.info(
        "[CACHE_LOAN_APP_CLIENT_PARTNER_DATA] Attempting to retrieve cached loan-client-partner"
            + " details. loanAppId={}, partnerId={}",
        loanApplicationId,
        partnerId);

    return redisCacheService
        .getObjectSilently(cacheKey, LoanClientPartnerMapEntity.class)
        .doOnNext(
            entity ->
                log.info(
                    "[CACHE_LOAN_APP_CLIENT_PARTNER_DATA] Cache HIT for loan-app-client-partner"
                        + " details. loanAppId={}, clientId={}, partnerId={}",
                    entity.getLoanApplicationId(),
                    entity.getClientId(),
                    entity.getPartnerId()))
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.info(
                        "[CACHE_LOAN_APP_CLIENT_PARTNER_DATA] Cache MISS for"
                            + " loan-app-client-partner details. loanAppId={}, partnerId={}",
                        loanApplicationId,
                        partnerId)))
        .doOnError(
            e ->
                log.error(
                    "[CACHE_LOAN_APP_CLIENT_PARTNER_DATA] Error fetching from cache."
                        + " loanAppId={}, partnerId={}, error={}",
                    loanApplicationId,
                    partnerId,
                    e.getMessage()));
  }

  public Mono<Void> cacheLoanAccountPartner(LoanAccountPartnerEntity entity) {
    log.info(
        "[CACHE_LOAN_ACCOUNT_PARTNER_DATA] Attempting to cache loan-account-partner details."
            + "lanId={} partnerId={}",
        entity.getLanId(),
        entity.getPartnerId());

    String cacheKey = buildLoanAccountPartnerCacheKey(entity.getLanId());

    return redisCacheService
        .cacheObjectSilently(cacheKey, entity, loanApplicationClientPartnerTTLSeconds)
        .doOnSuccess(
            unused ->
                log.info(
                    "[CACHE_LOAN_ACCOUNT_PARTNER_DATA] Successfully cached"
                        + " loan-account-partner details. lanId={},"
                        + " partnerId={}",
                    entity.getLanId(),
                    entity.getPartnerId()))
        .doOnError(
            e ->
                log.error(
                    "[CACHE_LOAN_ACCOUNT_PARTNER_DATA] Failed to cache loan-account-partner"
                        + " details. lanId={}, partnerId={}",
                    entity.getLanId(),
                    entity.getPartnerId()));
  }

  private String buildLoanApplicationClientPartnerCacheKey(String loanApplicationId) {
    return LAP_CLIENT_PARTNER_REDIS_KEY_PREFIX + ":" + loanApplicationId;
  }

  private String buildLoanAccountPartnerCacheKey(String lanId) {
    return LAN_PARTNER_REDIS_KEY_PREFIX + ":" + lanId;
  }

  public Mono<Void> cacheCreditLinePartner(CreditLinePartnerEntity entity) {
    log.info(
        "[CACHE_CREDIT_LINE_PARTNER_DATA] Attempting to cache credit-line-partner details."
            + " lineId={} partnerId={}",
        entity.getLineId(),
        entity.getPartnerId());

    String cacheKey = buildCreditLinePartnerCacheKey(entity.getLineId());

    return redisCacheService
        .cacheObjectSilently(cacheKey, entity, loanApplicationClientPartnerTTLSeconds)
        .doOnSuccess(
            unused ->
                log.info(
                    "[CACHE_CREDIT_LINE_PARTNER_DATA] Successfully cached"
                        + " credit-line-partner details. lineId={},"
                        + " partnerId={}",
                    entity.getLineId(),
                    entity.getPartnerId()))
        .doOnError(
            e ->
                log.error(
                    "[CACHE_CREDIT_LINE_PARTNER_DATA] Failed to cache credit-line-partner"
                        + " details. lineId={}, partnerId={}",
                    entity.getLineId(),
                    entity.getPartnerId()));
  }

  private String buildCreditLinePartnerCacheKey(String lineId) {
    return CREDIT_LINE_PARTNER_REDIS_KEY_PREFIX + ":" + lineId;
  }

  /** Retrieve cached credit-line-partner details from Redis. */
  public Mono<CreditLinePartnerEntity> getCreditLinePartner(String lineId) {
    String cacheKey = buildCreditLinePartnerCacheKey(lineId);

    log.info(
        "[CACHE_CREDIT_LINE_PARTNER_DATA] Attempting to retrieve cached credit-line-partner"
            + " details. lineId={}",
        lineId);

    return redisCacheService
        .getObjectSilently(cacheKey, CreditLinePartnerEntity.class)
        .doOnNext(
            entity ->
                log.info(
                    "[CACHE_CREDIT_LINE_PARTNER_DATA] Cache HIT for credit-line-partner"
                        + " details. lineId={}, partnerId={}, leadId={}",
                    entity.getLineId(),
                    entity.getPartnerId(),
                    entity.getLeadId()))
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.info(
                        "[CACHE_CREDIT_LINE_PARTNER_DATA] Cache MISS for"
                            + " credit-line-partner details. lineId={}",
                        lineId)))
        .doOnError(
            e ->
                log.error(
                    "[CACHE_CREDIT_LINE_PARTNER_DATA] Error fetching from cache."
                        + " lineId={}, error={}",
                    lineId,
                    e.getMessage()));
  }
}
