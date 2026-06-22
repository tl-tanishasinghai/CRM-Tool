package com.trillionloans.los.service;

import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.model.entity.CreditLinePartnerEntity;
import com.trillionloans.los.model.entity.LoanAccountPartnerEntity;
import com.trillionloans.los.model.entity.LoanApplicationClientPartnerEntity;
import com.trillionloans.los.model.entity.LoanClientPartnerMapEntity;
import com.trillionloans.los.model.response.LoanClientPartnerMapResponse;
import com.trillionloans.los.repository.CreditLineRepository;
import com.trillionloans.los.repository.LoanClientPartnerMapRepository;
import com.trillionloans.los.repository.PartnerMasterRepository;
import com.trillionloans.los.repository.drawdown.DrawdownAdditionalDetailsRepository;
import com.trillionloans.los.repository.drawdown.DrawdownRepository;
import com.trillionloans.los.util.LoanDataUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class LspService {

  private final LoanApplicationService loanApplicationService;
  private final PartnerMasterRepository partnerMasterRepository;
  private final LoanApplicationCacheService loanApplicationCacheService;
  private final LoanClientPartnerMapRepository clientPartnerMapRepository;
  private final LoanClientLookupService loanClientLookupService;
  private final DrawdownAdditionalDetailsRepository drawdownAdditionalDetailsRepository;
  private final DrawdownRepository drawdownRepository;
  private final CreditLineRepository creditLineRepository;

  public Mono<?> getPartnerMappingByLoanApplicationId(@PathVariable String loanApplicationId) {

    return loanClientLookupService
        .getPartnerMappingForLoanApplicationId(loanApplicationId)
        // CASE 1: Mapping already exists
        .flatMap(
            entity ->
                loanApplicationCacheService
                    .cacheLoanApplicationClientPartner(
                        LoanApplicationClientPartnerEntity.builder()
                            .loanApplicationId(loanApplicationId)
                            .clientId(String.valueOf(entity.getClientId()))
                            .partnerId(String.valueOf(entity.getPartnerId()))
                            .build())
                    .onErrorResume(
                        ex -> {
                          log.error(
                              "[LOAN_APP_CLIENT_PARTNER_MAP] failed to cache"
                                  + " LoanClientPartnerMapEntity for loanApplicationId={}",
                              loanApplicationId,
                              ex);
                          return Mono.empty(); // swallow error
                        })
                    .thenReturn(
                        LoanClientPartnerMapResponse.builder()
                            .loanApplicationId(entity.getLoanApplicationId())
                            .clientId(entity.getClientId())
                            .partnerId(entity.getPartnerId())
                            .build()))

        // CASE 2: Mapping not found → fallback flow
        .switchIfEmpty(
            loanApplicationService
                .getDetailsByLoanId(loanApplicationId)
                .flatMap(
                    dto -> {
                      String productKey = dto.getLosProductKey();

                      Mono<Integer> partnerIdMono =
                          "ELTO".equalsIgnoreCase(productKey)
                              ? Mono.just(1001)
                              : partnerMasterRepository
                                  .findPartnerIdByProductCode(productKey)
                                  .map(Integer::valueOf);

                      return partnerIdMono.flatMap(
                          partnerIdInt -> {
                            LoanClientPartnerMapEntity mapEntity =
                                LoanClientPartnerMapEntity.builder()
                                    .loanApplicationId(dto.getLoanApplicationId())
                                    .clientId(dto.getClientId())
                                    .partnerId(partnerIdInt)
                                    .build();

                            return clientPartnerMapRepository
                                .save(mapEntity)
                                .onErrorResume(
                                    e -> {
                                      log.error(
                                          "[LOAN_APP_CLIENT_PARTNER_MAP] failed to save"
                                              + " LoanClientPartnerMapEntity for"
                                              + " loanApplicationId={}",
                                          loanApplicationId,
                                          e);
                                      return Mono.empty(); // swallow error
                                    })
                                .then(
                                    loanApplicationCacheService
                                        .cacheLoanApplicationClientPartner(
                                            LoanApplicationClientPartnerEntity.builder()
                                                .loanApplicationId(loanApplicationId)
                                                .clientId(String.valueOf(dto.getClientId()))
                                                .partnerId(String.valueOf(partnerIdInt))
                                                .build())
                                        .onErrorResume(
                                            ex -> {
                                              log.error(
                                                  "[LOAN_APP_CLIENT_PARTNER_MAP] failed to cache"
                                                      + " LoanClientPartnerMapEntity for"
                                                      + " loanApplicationId={}",
                                                  loanApplicationId,
                                                  ex);
                                              return Mono.empty(); // swallow error
                                            }))
                                .thenReturn(
                                    LoanClientPartnerMapResponse.builder()
                                        .loanApplicationId(mapEntity.getLoanApplicationId())
                                        .clientId(mapEntity.getClientId())
                                        .partnerId(mapEntity.getPartnerId())
                                        .build());
                          });
                    }));
  }

  public Mono<?> getPartnerMappingByLanId(String lanId, String productCode) {

    // Credit Line products: lookup from drawdown tables
    if (LoanDataUtil.isCreditLineProduct(productCode)) {
      return getPartnerMappingForCreditLine(lanId);
    }

    // Non-Credit Line products: existing flow
    return getPartnerMappingForNonCreditLine(lanId);
  }

  /**
   * Gets partner mapping for Credit Line products. Flow: LAN → drawdown_additional_details →
   * drawdowns → partnerId
   */
  private Mono<?> getPartnerMappingForCreditLine(String lanId) {
    Long loanAccountNumber = Long.parseLong(lanId);

    return drawdownAdditionalDetailsRepository
        .findByLoanAccountNumber(loanAccountNumber)
        .flatMap(
            additionalDetails ->
                drawdownRepository
                    .findById(additionalDetails.getDrawdownId())
                    .flatMap(
                        drawdown -> {
                          String partnerId = drawdown.getPartnerId();

                          return loanApplicationCacheService
                              .cacheLoanAccountPartner(
                                  LoanAccountPartnerEntity.builder()
                                      .lanId(lanId)
                                      .partnerId(partnerId)
                                      .build())
                              .onErrorResume(
                                  ex -> {
                                    log.error(
                                        "[CREDIT_LINE_LAN_PARTNER_MAP] failed to cache for"
                                            + " lanId={}",
                                        lanId,
                                        ex);
                                    return Mono.empty(); // swallow error
                                  })
                              .thenReturn(
                                  LoanClientPartnerMapResponse.builder()
                                      .lanId(Integer.valueOf(lanId))
                                      .partnerId(Integer.parseInt(partnerId))
                                      .build());
                        }))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "[CREDIT_LINE_LAN_PARTNER_MAP] No drawdown data found for lanId={}", lanId);
                  return Mono.error(
                      new NotFoundException("No drawdown data found for lanId: " + lanId));
                }));
  }

  /** Gets partner mapping for Non-Credit Line products using existing flow. */
  private Mono<?> getPartnerMappingForNonCreditLine(String lanId) {
    return loanClientLookupService
        .getPartnerMappingForLanId(lanId)
        // CASE 1: Mapping already exists
        .flatMap(
            entity ->
                loanApplicationCacheService
                    .cacheLoanAccountPartner(
                        LoanAccountPartnerEntity.builder()
                            .lanId(lanId)
                            .partnerId(String.valueOf(entity.getPartnerId()))
                            .build())
                    .onErrorResume(
                        ex -> {
                          log.error(
                              "[LAN_CLIENT_PARTNER_MAP] failed to cache LoanClientPartnerMapEntity"
                                  + " for lanId={}",
                              lanId,
                              ex);
                          return Mono.empty(); // swallow error
                        })
                    .thenReturn(
                        LoanClientPartnerMapResponse.builder()
                            .loanApplicationId(entity.getLoanApplicationId())
                            .lanId(entity.getLanId())
                            .clientId(entity.getClientId())
                            .partnerId(entity.getPartnerId())
                            .build()))

        // CASE 2: Mapping not found → fallback flow
        .switchIfEmpty(
            loanApplicationService
                .getLoanApplicationByLanId(lanId)
                .flatMap(
                    dto -> {
                      String productKey = dto.getLosProductKey();

                      Mono<Integer> partnerIdMono =
                          "ELTO".equalsIgnoreCase(productKey)
                              ? Mono.just(1001)
                              : partnerMasterRepository
                                  .findPartnerIdByProductCode(productKey)
                                  .map(Integer::valueOf);

                      return partnerIdMono.flatMap(
                          partnerIdInt -> {
                            LoanClientPartnerMapEntity mapEntity =
                                LoanClientPartnerMapEntity.builder()
                                    .loanApplicationId(dto.getLoanApplicationId())
                                    .lanId(dto.getLanId())
                                    .clientId(dto.getClientId())
                                    .partnerId(partnerIdInt)
                                    .build();

                            return clientPartnerMapRepository
                                .findByLoanApplicationId(mapEntity.getLoanApplicationId())
                                .flatMap(
                                    existing -> {
                                      // Already present -> do nothing
                                      if (existing.getLanId() != null) {
                                        return Mono.just(existing);
                                      }
                                      // Update existing record
                                      existing.setLanId(mapEntity.getLanId());
                                      return clientPartnerMapRepository.save(existing);
                                    })
                                .switchIfEmpty(
                                    // Insert new record if not exists
                                    clientPartnerMapRepository.save(mapEntity))
                                .onErrorResume(
                                    e -> {
                                      log.error(
                                          "[LAN_CLIENT_PARTNER_MAP] failed to save"
                                              + " LoanClientPartnerMapEntity for lanId={}",
                                          lanId,
                                          e);
                                      return Mono.empty(); // swallow error
                                    })
                                .then(
                                    loanApplicationCacheService
                                        .cacheLoanAccountPartner(
                                            LoanAccountPartnerEntity.builder()
                                                .lanId(lanId)
                                                .partnerId(String.valueOf(partnerIdInt))
                                                .build())
                                        .onErrorResume(
                                            ex -> {
                                              log.error(
                                                  "[LAN_CLIENT_PARTNER_MAP] failed to cache"
                                                      + " LoanClientPartnerMapEntity for lanId={}",
                                                  lanId,
                                                  ex);
                                              return Mono.empty(); // swallow error
                                            }))
                                .thenReturn(
                                    LoanClientPartnerMapResponse.builder()
                                        .loanApplicationId(mapEntity.getLoanApplicationId())
                                        .lanId(mapEntity.getLanId())
                                        .clientId(mapEntity.getClientId())
                                        .partnerId(mapEntity.getPartnerId())
                                        .build());
                          });
                    }));
  }

  /**
   * Gets partner mapping for a credit line by lineId. Flow: lineId → credit_line (by
   * m2p_credit_line_id) → productCode → partner_master → partnerId
   */
  public Mono<?> getPartnerMappingByLineId(String lineId) {
    return creditLineRepository
        .findByM2pCreditLineId(lineId)
        .flatMap(
            creditLineEntity -> {
              String productCode = creditLineEntity.getProductCode();
              String leadId = creditLineEntity.getLeadId();

              // TODO: Get this from cache
              return partnerMasterRepository
                  .findPartnerIdByProductCode(productCode)
                  .flatMap(
                      partnerId -> {
                        String partnerIdStr = String.valueOf(partnerId);

                        return loanApplicationCacheService
                            .cacheCreditLinePartner(
                                CreditLinePartnerEntity.builder()
                                    .lineId(lineId)
                                    .partnerId(partnerIdStr)
                                    .leadId(leadId)
                                    .build())
                            .onErrorResume(
                                ex -> {
                                  log.error(
                                      "[CREDIT_LINE_PARTNER_MAP] failed to cache for lineId={}",
                                      lineId,
                                      ex);
                                  return Mono.empty(); // swallow error
                                })
                            .thenReturn(
                                LoanClientPartnerMapResponse.builder()
                                    .lineId(lineId)
                                    .partnerId(Integer.valueOf(partnerId))
                                    .build());
                      });
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "[CREDIT_LINE_PARTNER_MAP] No credit line data found for lineId={}", lineId);
                  return Mono.error(
                      new NotFoundException("No credit line data found for lineId: " + lineId));
                }));
  }
}
