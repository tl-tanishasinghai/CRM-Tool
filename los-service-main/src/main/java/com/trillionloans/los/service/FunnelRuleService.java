package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.LOAN_REJECTION;
import static com.trillionloans.los.constant.StringConstants.RISK_DEDUPE_ERROR;
import static com.trillionloans.los.constant.StringConstants.LOAN_CREATE_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG;
import static com.trillionloans.los.constant.StringConstants.FUNNELS_OFF_ERROR;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.config.BharatPeProductConfig;
import com.trillionloans.los.config.RiskCodeConfig;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.internal.DrawdownValidationResultDTO;
import com.trillionloans.los.model.entity.PartnerMasterEntity;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.request.LoanApplication;
import com.trillionloans.los.model.response.m2p.M2pLoanCreationResponseDTO;
import com.trillionloans.los.model.response.m2p.RiskDedupeValidationResponseDto;
import com.trillionloans.los.service.db.PartnerMasterService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import io.micrometer.common.util.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class FunnelRuleService {

  private final M2PWrapperApi m2PWrapperApi;
  private final RiskCodeConfig riskCodeConfig;
  private final PartnerMasterService partnerMasterService;
  private final Environment environment;
  private final BharatPeProductConfig bharatPeProductConfig;
  private final ProductConfigMasterService productConfigMasterService;

  public Mono<M2pLoanCreationResponseDTO> applyRiskDedupeChecks(
      LoanApplication loanData,
      String leadId,
      String productCode,
      boolean ctaRegistrationRequested,
      Supplier<Mono<M2pLoanCreationResponseDTO>> proceedWithLoanApplicationSupplier,
      AtomicReference<String> rejectionReason) {

    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productConfigTuple -> {
              ProductControl productControl = productConfigTuple.getT2();

              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControl, LOAN_CREATE_CTA_IDENTIFIER);

              if (flowData == null) {
                return Mono.error(
                    new BaseException(
                        SOMETHING_WENT_WRONG,
                        SOMETHING_WENT_WRONG,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }

              if (flowData.isDisableFunnel()) {
                log.error("[{}] Rejected: Funnel is turned off for productCode: {}", LOAN_REJECTION, productCode);
                rejectionReason.set("Rejected: Funnel is turned off for productCode: " + productCode);
                return Mono.error(new BaseException(FUNNELS_OFF_ERROR, FUNNELS_OFF_ERROR, HttpStatus.BAD_REQUEST));
              }

              return m2PWrapperApi
                  .getRiskDedupeValidationData(leadId)
                  .flatMap(
                      validationData ->
                          processRiskDedupeValidation(
                              validationData,
                              loanData,
                              proceedWithLoanApplicationSupplier,
                              rejectionReason))
                  .ofType(M2pLoanCreationResponseDTO.class);
            });
  }

  private Mono<M2pLoanCreationResponseDTO> processRiskDedupeValidation(
      RiskDedupeValidationResponseDto validationData,
      LoanApplication loanData,
      Supplier<Mono<M2pLoanCreationResponseDTO>> proceedWithLoanApplicationSupplier,
      AtomicReference<String> rejectionReason) {

    // Step 1: Check UHR based rejection
    Mono<Void> uhrCheck = checkUhrRejection(validationData, rejectionReason);
    if (uhrCheck != null) {
      return uhrCheck.then(Mono.empty());
    }

    // Step 2: Check for NPA loans
    Mono<Void> npaCheck = checkNpaLoans(validationData, rejectionReason);
    if (npaCheck != null) {
      return npaCheck.then(Mono.empty());
    }

    // Step 3: Check active loans and limit lines
    return checkActiveLoansAndLimitLines(
        validationData, loanData, proceedWithLoanApplicationSupplier, rejectionReason);
  }

  private Mono<Void> checkUhrRejection(
      RiskDedupeValidationResponseDto validationData, AtomicReference<String> rejectionReason) {

    boolean isUhr =
        riskCodeConfig.isUnacceptableHighRisk(validationData.getParsedRiskCategorisation());

    if (isUhr) {
      log.error("[{}] Rejected: UHR Based Rejection", LOAN_REJECTION);
      rejectionReason.set("Rejected: UHR Based Rejection");
      return Mono.error(
          new BaseException(RISK_DEDUPE_ERROR, RISK_DEDUPE_ERROR, HttpStatus.BAD_REQUEST));
    }

    return null;
  }

  private Mono<Void> checkNpaLoans(
      RiskDedupeValidationResponseDto validationData, AtomicReference<String> rejectionReason) {

    List<RiskDedupeValidationResponseDto.NpaLoan> npaLoans = validationData.getParsedNpaLoans();

    if (npaLoans != null && !npaLoans.isEmpty()) {
      log.error("[{}] Rejected: Has NPA Loan", LOAN_REJECTION);
      rejectionReason.set("Rejected: Has NPA Loan");
      return Mono.error(
          new BaseException(RISK_DEDUPE_ERROR, RISK_DEDUPE_ERROR, HttpStatus.BAD_REQUEST));
    }

    return null;
  }

  private Mono<M2pLoanCreationResponseDTO> checkActiveLoansAndLimitLines(
      RiskDedupeValidationResponseDto validationData,
      LoanApplication loanData,
      Supplier<Mono<M2pLoanCreationResponseDTO>> proceedWithLoanApplicationSupplier,
      AtomicReference<String> rejectionReason) {

    List<RiskDedupeValidationResponseDto.ActiveLoan> activeLoans =
        validationData.getParsedActiveLoans();
    List<RiskDedupeValidationResponseDto.ActiveLimitLine> activeLimitLime =
        validationData.getParsedActiveLimitLine();

    boolean hasActiveLoans = activeLoans != null && !activeLoans.isEmpty();
    boolean hasActiveLimitLine = activeLimitLime != null && !activeLimitLime.isEmpty();

    // Case 1: If both activeLoans and activeLimitLine are empty, proceed with loan creation
    if (!hasActiveLoans && !hasActiveLimitLine) {
      log.info("Proceed with Loan/Limit Creation Since No active Loan/Limit");
      return proceedWithLoanApplicationSupplier.get();
    }

    // Case 2: If activeLimitLine is not empty , reject Loan/Limit creation
    if (hasActiveLimitLine) {
      log.error("[{}] Rejected: Has Active Limit Line", LOAN_REJECTION);
      rejectionReason.set("Rejected: Has Active Limit Line");
      return Mono.error(
          new BaseException(RISK_DEDUPE_ERROR, RISK_DEDUPE_ERROR, HttpStatus.BAD_REQUEST));
    }

    // Case 2: If activeLimitLine is empty and activeLoans is not empty, continue with validation
    return partnerMasterService
        .findByProductCode(
            loanData.getLosProductKey().equalsIgnoreCase("ELTO")
                ? "ELO"
                : loanData.getLosProductKey())
        .flatMap(
            currentPartner ->
                validateActiveLoansAndProceed(
                    activeLoans,
                    loanData,
                    currentPartner,
                    rejectionReason,
                    proceedWithLoanApplicationSupplier));
  }

  private Mono<M2pLoanCreationResponseDTO> validateActiveLoansAndProceed(
      List<RiskDedupeValidationResponseDto.ActiveLoan> activeLoans,
      LoanApplication loanData,
      PartnerMasterEntity currentPartner,
      AtomicReference<String> rejectionReason,
      Supplier<Mono<M2pLoanCreationResponseDTO>> proceedWithLoanApplicationSupplier) {

    // Process each active loan to get partner information
    int bpDpd = Integer.parseInt(environment.getProperty("risk.dedupe.bp.dpd", "1"));
    int nonBpDpd = Integer.parseInt(environment.getProperty("risk.dedupe.nonBp.dpd", "0"));

    return Flux.fromIterable(activeLoans)
        .flatMap(
            activeLoan ->
                partnerMasterService
                    .findByM2pProductId(
                        activeLoan
                                .getLosProductKey()
                                .equals(environment.getProperty("product.elto.id"))
                            ? environment.getProperty("product.elo.id")
                            : activeLoan.getLosProductKey())
                    .onErrorResume(
                        e -> {
                          log.error("[{}] Due to Partner not found", LOAN_REJECTION);
                          rejectionReason.set("Due to Partner not found");
                          return Mono.error(
                              new BaseException(
                                  RISK_DEDUPE_ERROR, RISK_DEDUPE_ERROR, HttpStatus.BAD_REQUEST));
                        })
                    .map(loanPartner -> Map.entry(activeLoan, loanPartner)))
        .collectList()
        .flatMap(
            entries -> {
              if (entries.isEmpty()) {
                log.info("Proceed with Loan Creation Since No active Loan");
                return proceedWithLoanApplicationSupplier.get();
              }

              boolean partnerMismatch = false;
              boolean hasDpdGreaterThanOne = false;
              boolean allDpdZero = true;
              boolean rejectForBharatPeCombination = false;
              String currentPartnerName = currentPartner.getPartnerName();

              for (var entry : entries) {
                int dpd = entry.getKey().getMaxDpd();
                int existingProdId = Integer.parseInt(entry.getKey().getLosProductKey());
                String existingProdCode = entry.getValue().getProductCode();
                String entryPartner = entry.getValue().getPartnerName();

                if (!entryPartner.equalsIgnoreCase(currentPartnerName)) {
                  partnerMismatch = true;
                }
                if (dpd > bpDpd) {
                  hasDpdGreaterThanOne = true;
                }
                String existingProduct = normalizeProduct(existingProdCode, existingProdId);
                // BharatPe logic applies ONLY if incoming product is BP
                if (isBharatPeProduct(loanData.getLosProductKey())) {
                  if (shouldRejectBharatPeCombination(
                      loanData.getLosProductKey(), existingProduct)) {
                    rejectForBharatPeCombination = true;
                  }
                } else {
                  // Non-BharatPe loans MUST have all dpd = 0
                  if (dpd > nonBpDpd) {
                    allDpdZero = false;
                  }
                }
              }

              if (partnerMismatch) {
                log.error("[{}] Rejected: Partner name mismatch in existing loans", LOAN_REJECTION);
                rejectionReason.set("Rejected: Partner name mismatch in existing loans");
                return Mono.error(
                    new BaseException(
                        RISK_DEDUPE_ERROR, RISK_DEDUPE_ERROR, HttpStatus.BAD_REQUEST));
              }
              if (hasDpdGreaterThanOne) {
                log.error("[{}] Rejected: At least one loan has DPD > 1", LOAN_REJECTION);
                rejectionReason.set("Rejected: At least one loan has DPD > 1");
                return Mono.error(
                    new BaseException(
                        RISK_DEDUPE_ERROR, RISK_DEDUPE_ERROR, HttpStatus.BAD_REQUEST));
              }
              // Reject BharatPe invalid combinations
              if (rejectForBharatPeCombination) {
                log.error("[{}] Rejected:  Invalid BharatPe product combination", LOAN_REJECTION);
                rejectionReason.set("Rejected: Invalid BharatPe product combination");
                return Mono.error(
                    new BaseException(
                        RISK_DEDUPE_ERROR, RISK_DEDUPE_ERROR, HttpStatus.BAD_REQUEST));
              }
              // Non-BharatPe: allow if all dpd = 0
              if (!isBharatPeProduct(loanData.getLosProductKey()) && allDpdZero) {
                log.info("Allowed: All loans have DPD = 0 and partner matched");
                return proceedWithLoanApplicationSupplier.get();
              }
              // SPECIAL BP CASE:
              // Incoming ELTO allowed ONLY if existing is ONLY ELO
              if (loanData.getLosProductKey().equalsIgnoreCase("ELTO")) {
                return proceedWithLoanApplicationSupplier.get();
              }
              log.error("[{}] Rejected: No matching approval condition", LOAN_REJECTION);
              rejectionReason.set("Rejected: No matching approval condition");
              return Mono.error(
                  new BaseException(RISK_DEDUPE_ERROR, RISK_DEDUPE_ERROR, HttpStatus.BAD_REQUEST));
            });
  }

  private boolean shouldRejectBharatPeCombination(String incoming, String existing) {
    boolean allowed =
        "ELTO".equals(incoming) && ("ELO".equals(existing) || "ELTO".equals(existing));
    return !allowed; // reject if NOT allowed
  }

  private boolean isBharatPeProduct(String product) {
    return bharatPeProductConfig.getProductSet().contains(product);
  }

  private String normalizeProduct(String productCode, Integer productId) {
    if (StringUtils.isNotBlank(productCode)) {
      String code = productCode.toUpperCase();

      // Special: ELTO sometimes comes incorrectly as ELO in partner master
      if (StringUtils.isNotBlank(code)
          && code.equals("ELO")
          && productId != null
          && productId
              == Integer.parseInt(
                  Objects.requireNonNull(environment.getProperty("product.elto.id")))) {
        return "ELTO";
      }

      return code;
    }
    return productCode;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> convertToHashMap(Object loanApplication) {
    if (loanApplication instanceof HashMap) {
      return (Map<String, Object>) loanApplication;
    } else {
      return new HashMap<>();
    }
  }

  public Mono<DrawdownValidationResultDTO> applyFunnelLogicForDrawdown(
      String limitId, String leadId) {
    return m2PWrapperApi
        .getLoanApplicationByLoanId(leadId)
        .map(this::convertToHashMap)
        .flatMap(
            loanApplicationMap -> {
              // Extract and validate clientId early to keep it in scope
              Object clientIdObj = loanApplicationMap.get("clientId");
              if (clientIdObj == null) {
                log.error("[DRAWDOWN_FUNNEL] ClientId not found for loanId: {}", leadId);
                return Mono.just(
                    DrawdownValidationResultDTO.builder().isValid(false).clientId(null).build());
              }

              String clientId = String.valueOf(clientIdObj);
              int drawdownDpd =
                  Integer.parseInt(environment.getProperty("risk.dedupe.drawdown.dpd", "0"));
              log.info("[DRAWDOWN_FUNNEL] Fetched clientId: {} for loanId: {}", clientId, leadId);

              if (!Boolean.parseBoolean(
                  environment.getProperty("risk.dedupe.drawdown.check", "true"))) {
                log.info(
                    "[DRAWDOWN_FUNNEL] Validation flag is off, skipping checks for loanId: {}",
                    leadId);
                return Mono.just(
                    DrawdownValidationResultDTO.builder().isValid(true).clientId(clientId).build());
              }

              return m2PWrapperApi
                  .getRiskDedupeValidationData(clientId)
                  .flatMap(
                      validationData -> {
                        if (validationData == null) {
                          log.error(
                              "[DRAWDOWN_FUNNEL] Validation data is null for clientId: {}",
                              clientId);
                          return Mono.just(
                              DrawdownValidationResultDTO.builder()
                                  .isValid(false)
                                  .clientId(clientId)
                                  .build());
                        }

                        AtomicReference<String> rejectionReason = new AtomicReference<>();

                        // Check UHR using existing function
                        Mono<Void> uhrCheck = checkUhrRejection(validationData, rejectionReason);
                        if (uhrCheck != null) {
                          log.error("[DRAWDOWN_FUNNEL] {}", rejectionReason.get());
                          return Mono.just(
                              DrawdownValidationResultDTO.builder()
                                  .isValid(false)
                                  .clientId(clientId)
                                  .build());
                        }

                        // Check NPA using existing function
                        Mono<Void> npaCheck = checkNpaLoans(validationData, rejectionReason);
                        if (npaCheck != null) {
                          log.error("[DRAWDOWN_FUNNEL] {}", rejectionReason.get());
                          return Mono.just(
                              DrawdownValidationResultDTO.builder()
                                  .isValid(false)
                                  .clientId(clientId)
                                  .build());
                        }

                        // Get active loans and limit lines
                        List<RiskDedupeValidationResponseDto.ActiveLoan> activeLoans =
                            validationData.getParsedActiveLoans();
                        List<RiskDedupeValidationResponseDto.ActiveLimitLine> activeLimitLines =
                            validationData.getParsedActiveLimitLine();

                        boolean hasActiveLoans = activeLoans != null && !activeLoans.isEmpty();
                        boolean hasActiveLimitLines =
                            activeLimitLines != null && !activeLimitLines.isEmpty();

                        // If there are active loans, reject
                        if (hasActiveLoans) {
                          log.error(
                              "[DRAWDOWN_FUNNEL] Rejected: Has active loans for clientId: {}",
                              clientId);
                          return Mono.just(
                              DrawdownValidationResultDTO.builder()
                                  .isValid(false)
                                  .clientId(clientId)
                                  .build());
                        }

                        // If only limits are present, check if all are from the same limit and DPD
                        // = 0
                        if (hasActiveLimitLines) {
                          // Check if all limit lines belong to the provided limitId
                          boolean allFromSameLimit =
                              activeLimitLines.stream()
                                  .allMatch(limitLine -> limitId.equals(limitLine.getLimitId()));

                          if (!allFromSameLimit) {
                            log.error(
                                "[DRAWDOWN_FUNNEL] Rejected: Not all Drawdown are from the same"
                                    + " line for clientId: {}, limitId: {}",
                                clientId,
                                limitId);
                            return Mono.just(
                                DrawdownValidationResultDTO.builder()
                                    .isValid(false)
                                    .clientId(clientId)
                                    .build());
                          }

                          // Check for DPD > 0
                          boolean hasLimitWithDpdGreaterThanZero =
                              activeLimitLines.stream()
                                  .anyMatch(limitLine -> limitLine.getMaxDpd() > drawdownDpd);

                          if (hasLimitWithDpdGreaterThanZero) {
                            log.error(
                                "[DRAWDOWN_FUNNEL] Rejected: Has Drawdown with DPD > 0 for"
                                    + " clientId: {}",
                                clientId);
                            return Mono.just(
                                DrawdownValidationResultDTO.builder()
                                    .isValid(false)
                                    .clientId(clientId)
                                    .build());
                          }
                        }

                        // All checks passed
                        log.info(
                            "[DRAWDOWN_FUNNEL] Validation passed for clientId: {}, loanId: {}",
                            clientId,
                            leadId);
                        return Mono.just(
                            DrawdownValidationResultDTO.builder()
                                .isValid(true)
                                .clientId(clientId)
                                .build());
                      })
                  .onErrorResume(
                      error -> {
                        log.error(
                            "[DRAWDOWN_FUNNEL] Error during validation for clientId: {}, loanId:"
                                + " {}, error: {}",
                            clientId,
                            leadId,
                            error.getMessage());
                        return Mono.just(
                            DrawdownValidationResultDTO.builder()
                                .isValid(false)
                                .clientId(clientId)
                                .build());
                      });
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[DRAWDOWN_FUNNEL] Error fetching loan application for loanId: {}, error: {}",
                  leadId,
                  error.getMessage());
              return Mono.just(
                  DrawdownValidationResultDTO.builder().isValid(false).clientId(null).build());
            });
  }
}
